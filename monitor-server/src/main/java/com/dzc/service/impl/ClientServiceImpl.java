package com.dzc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzc.entity.dto.AccountMonitorSetting;
import com.dzc.entity.dto.Client;
import com.dzc.entity.dto.ClientDetail;
import com.dzc.entity.vo.request.*;
import com.dzc.entity.vo.response.*;
import com.dzc.mapper.AccountMonitorSettingMapper;
import com.dzc.mapper.ClientDetailMapper;
import com.dzc.mapper.ClientMapper;
import com.dzc.service.ClientService;
import com.dzc.utils.Const;
import com.dzc.utils.InfluxDbUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {
    @Resource
    ClientDetailMapper clientDetailMapper;

    @Resource
    AccountMonitorSettingMapper accountMonitorSettingMapper;

    @Resource
    InfluxDbUtils influxDbUtils;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RabbitTemplate rabbitTemplate;

    private String registerToken = this.generateNewToken();

    private final Map<String, Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String, Client> clientTokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initClientCache() {
        clientIdCache.clear();
        clientTokenCache.clear();
        this.list().forEach(this::addClientCache);
    }

    @Override
    public boolean verifyAndRegister(String token) {
        if (this.registerToken.equals(token)) {
            String id = this.randomClientId();
            Client client = new Client(id, "未命名主机", token, "未命名节点", new Date());
            if (this.save(client)) {
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    @Override
    public Client findClientById(String id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }

    @Override
    public String registerToken() {
        return registerToken;
    }

    @Override
    public void updateClientDetail(Client client, ClientDetailVO clientDetailVO) {
        ClientDetail clientDetail = new ClientDetail();
        BeanUtils.copyProperties(clientDetailVO, clientDetail);
        clientDetail.setId(client.getId());
        if (Objects.nonNull(clientDetailMapper.selectById(client.getId()))) {
            clientDetailMapper.updateById(clientDetail);
        } else {
            clientDetailMapper.insert(clientDetail);
        }
    }

    private final Map<String, RuntimeDetailVO> currentRuntime = new ConcurrentHashMap<>();

    @Override
    public void updateRunTimeDetail(Client client, RuntimeDetailVO runtimeDetailVO) {
        currentRuntime.put(client.getId(), runtimeDetailVO);
        influxDbUtils.writeRuntimeData(client.getId(), runtimeDetailVO);
    }

    @Override
    public void updatePortDetail(Client client, PortDetailVO portDetailVO) {
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":http:", String.valueOf(portDetailVO.isHttpStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":https:", String.valueOf(portDetailVO.isHttpsStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":ftp:", String.valueOf(portDetailVO.isFtpStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":sftp:", String.valueOf(portDetailVO.isSftpStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":ftps:", String.valueOf(portDetailVO.isFtpsStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":smtp:", String.valueOf(portDetailVO.isSmtpStatus()));
        stringRedisTemplate.opsForValue().set(Const.PORT_STATUS + client.getId() + ":timestamp:", String.valueOf(portDetailVO.getTimestamp()));
    }

    @Override
    public List<ClientPreviewVO> listClients(int userId, String email) {
        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO clientPreviewVO = client.asViewObject(ClientPreviewVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(clientPreviewVO.getId()), clientPreviewVO);
            RuntimeDetailVO runtimeDetailVO = currentRuntime.get(client.getId());


            QueryWrapper<AccountMonitorSetting> queryWrapper = new QueryWrapper<>();
            AccountMonitorSetting accountMonitorSetting = accountMonitorSettingMapper.selectOne(queryWrapper
                    .eq("user_id", userId).eq("client_id", client.getId()));
            boolean isClientOnline = this.isOnline(runtimeDetailVO);
            if (isClientOnline) {
                BeanUtils.copyProperties(runtimeDetailVO, clientPreviewVO);
                clientPreviewVO.setOnline(true);
            }

            if (accountMonitorSetting != null) {
                if (accountMonitorSetting.isHost() && isClientOnline) {
                    //如果已经连通了，就退出黑名单
                    stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":host:");
                } else {
                    //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                    if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":host:"))) {
                        this.monitorEmail("host", email, client.getId());
                    }
                }
                if (accountMonitorSetting.isHttp()){
                    String http = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":http:");
                    if (Objects.equals(http, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":http:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":http:"))) {
                            this.monitorEmail("http", email, client.getId());
                        }
                    }
                }
                if (accountMonitorSetting.isHttps()){
                    String https = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":https:");
                    if (Objects.equals(https, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":https:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":https:"))) {
                            this.monitorEmail("https", email, client.getId());
                        }
                    }
                }
                if (accountMonitorSetting.isFtp()){
                    String ftp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":ftp:");
                    if (Objects.equals(ftp, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":ftp:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":ftp:"))) {
                            this.monitorEmail("ftp", email, client.getId());
                        }
                    }
                }
                if (accountMonitorSetting.isSftp()){
                    String http = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":sftp:");
                    if (Objects.equals(http, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":sftp:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":sftp:"))) {
                            this.monitorEmail("sftp", email, client.getId());
                        }
                    }
                }
                if (accountMonitorSetting.isFtps()){
                    String ftps = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":ftps:");
                    if (Objects.equals(ftps, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":ftps:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":ftps:"))) {
                            this.monitorEmail("ftps", email, client.getId());
                        }
                    }
                }
                if (accountMonitorSetting.isSmtp()){
                    String smtp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + client.getId() + ":smtp:");
                    if (Objects.equals(smtp, "true")) {
                        //如果已经连通了，就退出黑名单
                        stringRedisTemplate.delete(Const.MONITOR_BLACK_LIST + client.getId() + ":smtp:");
                    } else {
                        //如果没有连通，判断是否已经发送过消息了，如果已经发送过了一天之内就不再发消息了，除非重新连通则退出黑名单
                        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(Const.MONITOR_BLACK_LIST + client.getId() + ":smtp:"))) {
                            this.monitorEmail("smtp", email, client.getId());
                        }
                    }
                }
            }

            return clientPreviewVO;
        }).toList();
    }

    public void monitorEmail(String type, String email, String clientId) {
        Map<String, Object> data = Map.of("type", type, "email", email, "clientId", clientId);
        rabbitTemplate.convertAndSend(Const.MQ_MAIL, data);
        stringRedisTemplate.opsForValue()
                .set(Const.MONITOR_BLACK_LIST + clientId + ":" + type + ":", "", 1, TimeUnit.DAYS);
    }

    @Override
    public List<ClientSimpleVO> listSimpleDetails() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO clientSimpleVO = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(clientDetailMapper.selectById(client.getId()), clientSimpleVO);
            return clientSimpleVO;
        }).toList();
    }

    @Override
    public void renameClient(RenameClientVO renameClientVO) {
        this.update(Wrappers.<Client>update().eq("id", renameClientVO.getId()).set("name", renameClientVO.getName()));
        this.initClientCache();
    }

    @Override
    public void renameNode(RenameNodeVO renameNodeVO) {
        this.update(Wrappers.<Client>update().eq("id", renameNodeVO.getId())
                .set("node", renameNodeVO.getNode()));
        this.initClientCache();
    }

    @Override
    public RuntimeHistoryVO clientRuntimeDetailsHistory(String id) {
        System.out.println("id:" + id);
        RuntimeHistoryVO runtimeHistoryVO = influxDbUtils.readRuntimeData(id);
        System.out.println("runtimeHistoryVO:" + runtimeHistoryVO);
        ClientDetail clientDetail = clientDetailMapper.selectById(id);
        System.out.println("clientDetail:" + clientDetail);
        BeanUtils.copyProperties(clientDetail, runtimeHistoryVO);
        System.out.println("runtimeHistoryVO:" + runtimeHistoryVO);
        return runtimeHistoryVO;
    }

    @Override
    public RuntimeDetailVO clientRuntimeDetailsNow(String id) {
        return currentRuntime.get(id);
    }

    @Override
    public PortDetailVO clientPortDetails(String id) {
        PortDetailVO portDetailVO = new PortDetailVO();
        String http = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":http:");
        if (http == null) http = "false";
        portDetailVO.setHttpStatus(Boolean.parseBoolean(http));
        String https = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":https:");
        if (https == null) https = "false";
        portDetailVO.setHttpsStatus(Boolean.parseBoolean(https));
        String ftp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":ftp:");
        if (ftp == null) ftp = "false";
        portDetailVO.setFtpStatus(Boolean.parseBoolean(ftp));
        String sftp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":sftp:");
        if (sftp == null) sftp = "false";
        portDetailVO.setSftpStatus(Boolean.parseBoolean(sftp));
        String ftps = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":ftps:");
        if (ftps == null) ftps = "false";
        portDetailVO.setFtpsStatus(Boolean.parseBoolean(ftps));
        String smtp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":smtp:");
        if (smtp == null) smtp = "false";
        portDetailVO.setSmtpStatus(Boolean.parseBoolean(smtp));

        String timestamp = stringRedisTemplate.opsForValue().get(Const.PORT_STATUS + id + ":timestamp:");
        if (timestamp == null) timestamp = "";
        portDetailVO.setTimestamp(Long.parseLong(timestamp));

        return portDetailVO;
    }

    @Override
    public void deleteClient(String id) {
        this.removeById(id);
        clientDetailMapper.deleteById(id);
        initClientCache();
        currentRuntime.remove(id);
    }

    @Override
    public ClientDetailsVO getClientDetails(String id) {
        ClientDetailsVO clientDetailsVO = this.clientIdCache.get(id).asViewObject(ClientDetailsVO.class);
        BeanUtils.copyProperties(clientDetailMapper.selectById(id), clientDetailsVO);
        clientDetailsVO.setOnline(this.isOnline(currentRuntime.get(id)));
        return clientDetailsVO;
    }

    private boolean isOnline(RuntimeDetailVO runtimeDetailVO) {
        return runtimeDetailVO != null && (System.currentTimeMillis() - runtimeDetailVO.getTimestamp()) < 60 * 1000;
    }

    private void addClientCache(Client client) {
        clientIdCache.put(client.getId(), client);
        clientTokenCache.put(client.getToken(), client);
    }

    private String randomClientId() {
        return "C" + RandomStringUtils.random(11, false, true);
    }

    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        System.out.println(sb);
        return sb.toString();
    }
}
