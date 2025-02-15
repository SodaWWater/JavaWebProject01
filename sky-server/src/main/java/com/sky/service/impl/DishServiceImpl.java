package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 新增菜品
     * @param dishDTO
     */
//    开启AOP事务  因为涉及多个表的修改操作
   @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
       Dish dish = new Dish();
       BeanUtils.copyProperties(dishDTO,dish);
//        向菜品表插入一条数据
       dishMapper.insert(dish);
//       获取当前菜品主键值
       Long id = dish.getId();
//       向口味表插入n条数据
       List<DishFlavor> flavors = dishDTO.getFlavors();
       if (flavors != null && !flavors.isEmpty()){
           for (DishFlavor flavor : flavors) {
               flavor.setDishId(id);
           }
            dishFlavorMapper.insertBatch(flavors);
       }
   }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
//        分页
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page =  dishMapper.pageQuery(dishPageQueryDTO);
        List<DishVO> pages = page.getResult();
        return new PageResult(page.getTotal(),pages);
    }

    /**
     * 菜品批量删除
     * @param ids
     */
//    设计多表的修改 加入事务注解 保证数据一致性
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (StatusConstant.ENABLE.equals(dish.getStatus())){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除--是否被套餐表关联
        List<Long> setmealIds = setMealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        //删除菜品表中菜品数据
//        for (Long id : ids) {
//            dishMapper.deleteBatch(id);
//            //删除菜品表关联的口味表中数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
//        优化
        //删除菜品表中菜品数据

            dishMapper.deleteBatchs(ids);
            //删除菜品表关联的口味表中数据
            dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Transactional
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询菜品口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //将两类数据封装到DishVO对象中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改对应的菜品及口味数据
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //修改菜品表基本信息
        dishMapper.update(dish);
//        删除原有口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
//        重新插入口口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && !flavors.isEmpty()){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 起售/禁止售菜品
     * @param status
     * @param id
     */
    @Transactional
    @Override
    public void starOrStop(Integer status, Long id) {
        if (status != null && id != null){
//            设置当前菜品状态
            Dish dish = Dish.builder()
                    .status(status)
                    .id(id)
                    .build();
            dishMapper.update(dish);
//            如果菜品为禁止售，则还需禁售对应套餐
            if ( StatusConstant.DISABLE.equals(status)){
                List<Long> ids =new ArrayList<>();
                ids.add(id);
//                查询当前菜品对应的套餐id
                List<Long> mealsId = setMealDishMapper.getSetmealIdsByDishIds(ids);
//                如果查询结果值存在，则将对应套餐禁售
                if ( mealsId != null && !mealsId.isEmpty()){
                    for (Long mId : mealsId) {
                        Setmeal setmeal = Setmeal.builder()
                                .id(mId)
                                .status(StatusConstant.ENABLE)
                                .build();
                        setmealMapper.update(setmeal);
                    }
                }
            }
        }else {
            throw new BaseException("起售停售菜品方法参数有误");
        }
    }

    /**
     * 根据菜品分类id查询菜品
     * @param categoryId
     */
    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }


    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
