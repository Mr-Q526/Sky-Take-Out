package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import nonapi.io.github.classgraph.utils.Join;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面 用于实现公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */

    // 拦截Mapper中 有AutoFill注解的方法
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    // 拦截到之后
    public void autoFillPointCut(){

    }

    /**
     * 前置通知 用于实现公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行数据填充" + joinPoint.getSignature());

        // 获取到当前被拦截的方法上的数据库操作类型
        // 获得签名 强转为 方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // 获取当前方法上的注解对象
        OperationType operationType = autoFill.value();// 获取操作类型

        // 获取当前被拦截方法的参数 - 实体对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return ;
        }

        Object entity = args[0];// 获取方法参数 我们规定把他放到第一个位置
        // 使用 Object 类型， 因为 实体对象可能是任何对象， 不知道是什么对象

        // 准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long id = BaseContext.getCurrentId();

        // 根据当前不同的操作类型 为对应的属性通过 反射 来赋值 (insert update 需要赋值的变量不一样)
        if(operationType == OperationType.INSERT){
            //如果是insert类型 需要为四个公共字段赋值

            // 获得方法的set方法
            try{
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射 为对象属性赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, id);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if(operationType == OperationType.UPDATE){
            // 如果是update ， 只赋值两个属性
            try{
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
