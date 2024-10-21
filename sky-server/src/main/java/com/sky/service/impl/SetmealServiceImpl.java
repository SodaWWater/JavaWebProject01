package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
//        新增套餐的同时还需新增套餐_菜品表
//        1.分别创建套餐类对象，套餐菜品类对象(一个套餐可能对应多个菜品) 并赋值
        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealDTO, setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

//        2.插入套餐信息
        setmealMapper.insert(setmeal);
//        将生成的套餐id插入套餐菜品表
        Long id = setmeal.getId();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
//        3.插入套餐_菜品表信息
        if (!setmealDishes.isEmpty()) {
            setMealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 分页查询套餐信息
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> pages = setmealMapper.pageQuery(setmealPageQueryDTO);
        List<SetmealVO> result = pages.getResult();
        long total = pages.getTotal();

        return new PageResult(total, result);
    }

    /**
     * 批量删除套餐信息
     *
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
//        根据id查询套餐是否存在
        List<Setmeal> setmeals = setmealMapper.selectByIds(ids);
        if (setmeals.isEmpty()) {
            throw new BaseException("套餐id为空，套餐不存在。");
        }
//        查询套餐是否在售
        for (Setmeal setmeal : setmeals) {
            if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                throw new BaseException("套餐在售,不可删除");
            }
        }

//        根据套餐主键id删除对应套餐
        setmealMapper.deleteByIds(ids);
//        根据套餐id删除套餐对应的套餐菜品表
        setMealDishMapper.deleteBySetmealIds(ids);

    }

    /**
     * 根据套餐id查询套餐信息
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO selectById(Long id) {
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(id);
//        根据套餐id查询 套餐信息 以及 套餐_菜品 信息
        List<Setmeal> setmeals = setmealMapper.selectByIds(ids);
        Setmeal setmeal = setmeals.get(0);
        List<SetmealDish> setmealDish = setMealDishMapper.selectByIds(ids);
//      将信息封装到setmealVO对象中
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDish);
        return setmealVO;
    }

    /**
     * 起售/仅售套餐
     *
     * @param status
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
//        通过套餐_菜品表查询对应菜品状态中 ，如果是禁售菜品 则不可起售套餐
        List<Dish> dishs = setMealDishMapper.selectDishBySetmealId(setmeal.getId());
        for (Dish dish : dishs) {
            if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                throw new BaseException("起售套餐中包含禁售菜品!");
            }
        }
        setmealMapper.update(setmeal);

    }


    /**
     * 修改套餐信息
     *
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
//        将套餐信息修改 并将套餐id返回
        setmealMapper.update(setmeal);
//        通过套餐id将套餐对应的套餐_菜品信息进行修改
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(setmeal.getId());
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
        }

//        (1)将原有套餐_菜品信息删除
        setMealDishMapper.deleteBySetmealIds(ids);
//        (2)将插入新的对应信息
        setMealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
