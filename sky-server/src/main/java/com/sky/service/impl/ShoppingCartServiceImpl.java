package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.BaseException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.properties.JwtProperties;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断当前购物车中是否已经存在当前菜品
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //如果存在则让份数+1即可
        if (list!= null && !list.isEmpty()){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            //如果不存在 进行数据库插入
            //1.判断插入的是单个菜品还是套餐
            Long dishId = shoppingCart.getDishId();
            if (dishId != null){
                //是菜品 进行菜品添加
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 套餐添加
                Long setmealId = shoppingCartDTO.getSetmealId();
                ArrayList<Long> ids = new ArrayList<>();
                ids.add(setmealId);
                List<Setmeal> setmeals = setmealMapper.selectByIds(ids);
                Setmeal setmeal = setmeals.get(0);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }


    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long currentId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(currentId)
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        Long currentId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(currentId);
    }

    /**
     * 清空购物车单个商品
     * @param shoppingCartDTO
     */
    @Override
    @Transactional
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
//        获取当前用户id
        Long currentId = BaseContext.getCurrentId();
//        查询当前购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(currentId);
//        查询当前要删除数据份数，》1 -1 ，《1 直接删除
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && !list.isEmpty()){
            shoppingCart = list.get(0);
        }
        Integer number = shoppingCart.getNumber();
        if (number == 1){
            shoppingCartMapper.deleteById(shoppingCart.getId());
        }else if(number > 1){
            shoppingCart.setNumber(number - 1);
            shoppingCartMapper.updateNumberById(shoppingCart);
        }else {
            throw  new BaseException("购物车商品数量数据异常");
        }

    }
}
