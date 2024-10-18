package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
