package com.yang.yangbi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yang.yangbi.annotation.AuthCheck;
import com.yang.yangbi.bizmq.BiMessageProducer;
import com.yang.yangbi.common.BaseResponse;
import com.yang.yangbi.common.DeleteRequest;
import com.yang.yangbi.common.ErrorCode;
import com.yang.yangbi.common.ResultUtils;
import com.yang.yangbi.constant.CommonConstant;
import com.yang.yangbi.constant.FileConstant;
import com.yang.yangbi.constant.UserConstant;
import com.yang.yangbi.exception.BusinessException;
import com.yang.yangbi.exception.ThrowUtils;
import com.yang.yangbi.manager.AiManager;
import com.yang.yangbi.manager.RedisLimiterManager;
import com.yang.yangbi.model.dto.chart.*;
import com.yang.yangbi.model.dto.file.UploadFileRequest;
import com.yang.yangbi.model.dto.post.PostQueryRequest;
import com.yang.yangbi.model.entity.Chart;
import com.yang.yangbi.model.entity.Post;
import com.yang.yangbi.model.entity.User;
import com.yang.yangbi.model.enums.FileUploadBizEnum;
import com.yang.yangbi.model.vo.BiResponse;
import com.yang.yangbi.service.ChartService;
import com.yang.yangbi.service.UserService;
import com.yang.yangbi.utils.ExcelUtils;
import com.yang.yangbi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 * @autho yangyang
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {


    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        List<String> tags = chartUpdateRequest.getTags();
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
//        if (chartQueryRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        System.out.println("a");
        Long userId = chartQueryRequest.getUserId();
        User loginUser = userService.getLoginUser(request);//好了
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
        chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
//    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
//        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
//        if (chartQueryRequest == null) {
//            return queryWrapper;
//        }
//        Long id = chartQueryRequest.getId();
//        String name = chartQueryRequest.getName();
//        String goal = chartQueryRequest.getGoal();
//        String chartType = chartQueryRequest.getChartType();
//        Long userId = chartQueryRequest.getUserId();
//        String sortField = chartQueryRequest.getSortField();
//        String sortOrder = chartQueryRequest.getSortOrder();
//
//        queryWrapper.eq(id != null && id > 0, "id", id);
//        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
//        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
//        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
//        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
//        queryWrapper.eq("isDelete", false);
//        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
//                sortField);
//        return queryWrapper;
//    }
    /**
     * 同步智能分析
     *
     * @param multipartFile
     * @param
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> GenChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //检验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 20, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        //校验文件大小
        ThrowUtils.throwIf(size > ONE_MB ,ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件的后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        //用户限流
        //每个用户应该限流器，每个用户的每个方法做一个限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //模AI型id
        long biModelId = 1659171950288818178l;
//        https://www.yucongming.com/model/1659171950288818178?inviteUser=1673347724642172929
        //构造用户输入用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += "请使用：" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");

        //读取用户上传的excel文件,压缩
        String result = ExcelUtils.excelToCvs(multipartFile);
        userInput.append("数据:").append(result).append("\n");

        //发送给AI并且返回结果
        String res = aiManager.doChat(biModelId, userInput.toString());
        String[] split = res.split("【【【【【");
        if (split.length < 3){
            new BusinessException(ErrorCode.PARAMS_ERROR,"AI生成错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();

        //插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setStatus("succeed");
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save,ErrorCode.PARAMS_ERROR,"图表保存失败");

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        return ResultUtils.success(biResponse);
    }


    /**
     * 异步智能分析,线程池技术
     *
     * @param multipartFile
     * @param
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> GenChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //检验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 20, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        //校验文件大小
        ThrowUtils.throwIf(size > ONE_MB ,ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件的后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        //用户限流
        //每个用户应该限流器，每个用户的每个方法做一个限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //模AI型id
        long biModelId = 1659171950288818178l;
//        https://www.yucongming.com/model/1659171950288818178?inviteUser=1673347724642172929
        //构造用户输入用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += "请使用：" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");

        //读取用户上传的excel文件,压缩
        String result = ExcelUtils.excelToCvs(multipartFile);
        userInput.append("数据:").append(result).append("\n");

        //插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save,ErrorCode.PARAMS_ERROR,"图表保存失败");

        CompletableFuture.runAsync(() -> {
            //先将图表任务改为“执行中”，等待执行成功后，修改为已完成，保存执行结果。执行失败后，状态修改为失败，记录任务失败信息
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean result1 = chartService.updateById(updateChart);
            if (!result1){
                handleChartUpdateError(updateChart.getId(),"图表状态更新失败");
                return;
            }

            //发送给AI并且返回结果
            String res = aiManager.doChat(biModelId, userInput.toString());
            String[] split = res.split("【【【【【");
            if (split.length < 3){
                handleChartUpdateError(chart.getId(),"AI生成错误");
                return;
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setGenResult(genResult);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setStatus("succeed");
            updateChartResult.setId(chart.getId());
            boolean b = chartService.updateById(updateChartResult);
            if (!b){
                handleChartUpdateError(updateChartResult.getId(),"更新图表成功状态失败");
                return;
            }
        },threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 异步智能分析,消息队列技术
     *
     * @param multipartFile
     * @param
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> GenChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //检验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 20, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        //校验文件大小
        ThrowUtils.throwIf(size > ONE_MB ,ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件的后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx","xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);
        //用户限流
        //每个用户应该限流器，每个用户的每个方法做一个限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        //模AI型id
        long biModelId = 1659171950288818178l;
//        https://www.yucongming.com/model/1659171950288818178?inviteUser=1673347724642172929
        //构造用户输入用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += "请使用：" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据").append("\n");

        //读取用户上传的excel文件,压缩
        String result = ExcelUtils.excelToCvs(multipartFile);
        userInput.append("数据:").append(result).append("\n");

        //插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save,ErrorCode.PARAMS_ERROR,"图表保存失败");
        Long ChartId = chart.getId();
        biMessageProducer.sentMessage(String.valueOf(ChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(ChartId);
        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(Long chartId,String exeMessage){
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExeMessage("exeMessage");
        boolean b = chartService.updateById(updateChartResult);
        if (!b){
            log.error("更新图表失败状态失败" );
        }
    }


//        User loginUser = userService.getLoginUser(request);
//        // 文件目录：根据业务、用户来划分
//        String uuid = RandomStringUtils.randomAlphanumeric(8);
//        String filename = uuid + "-" + multipartFile.getOriginalFilename();
//        File file = null;
//        try {
////            // 上传文件
////            file = File.createTempFile(filepath, null);
////            multipartFile.transferTo(file);
////            cosManager.putObject(filepath, file);
////            // 返回可访问地址
////            return ResultUtils.success(FileConstant.COS_HOST + filepath);
//        } catch (Exception e) {
////            log.error("file upload error, filepath = " + filepath, e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
//        } finally {
//            if (file != null) {
//                // 删除临时文件
//                boolean delete = file.delete();
//                if (!delete) {
////                    log.error("file delete error, filepath = {}", filepath);
//                }
//            }
//        }
//        return null;
//    }

}
