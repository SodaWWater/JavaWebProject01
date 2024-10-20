package com.sky.mapper;

import com.sky.entity.Dish;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface SetMealDishMapper {

    /**
     * 根据菜品id查询对应套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 新增套餐_菜品 数据
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除对应套餐_菜品数据
     * @param ids
     */
    void deleteBySetmealIds(List<Long> ids);

    /**
     * 根据套餐id查询套餐_菜品信息
     * @param ids
     * @return
     */
    List<SetmealDish> selectByIds(ArrayList<Long> ids);

    /**
     * 通过套餐id 在套餐_菜品表中查询 菜品id 再查询菜品对应状态
     * @param id
     */
    List<Dish> selectDishBySetmealId(Long id);
}
