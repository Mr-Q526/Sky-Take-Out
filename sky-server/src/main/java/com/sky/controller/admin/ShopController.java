package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getSatus(){
        log.info("获取店铺营业状态");
        Integer status =shopService.getStatus();
        return Result.success(status);
    }

    @PutMapping("/{status}")
    @ApiOperation("修改店铺营业状态")
    public Result updateStatus(@PathVariable Integer status){
        log.info("修改店铺营业状态：{}", status == 1 ? "营业中" : "打烊中");
        shopService.updateStatus(status);
        return Result.success();
    }


}
