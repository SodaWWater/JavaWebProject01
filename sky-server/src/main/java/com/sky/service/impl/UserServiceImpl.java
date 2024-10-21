package com.sky.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

//    微信服务接口地址
    public static final  String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    /**
     * 微信用户登陆
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
//      调用微信服务接口获取当前用户openid
        String openid = getOpenid(userLoginDTO.getCode());

//      判断openid是否为空。空 -》登陆失败 抛出异常
        if (null == openid){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
//      判断当前用户是否微信用户
        User user = userMapper.getByOpenid(openid);
        if (null == user){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
//      如果是则添加用户信息自动完成注册
//      返回当前用户对象
        return user;
    }

    private String getOpenid(String code){
        //      调用微信服务接口获取当前用户openid

        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");

        String s = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(s);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
