package com.dzc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzc.entity.dto.Client;
import com.dzc.entity.vo.request.*;
import com.dzc.entity.vo.response.*;

import java.util.List;

public interface ClientService extends IService<Client> {
    boolean verifyAndRegister(String token);

    Client findClientById(String id);

    Client findClientByToken(String token);

    String registerToken();

    void updateClientDetail(Client client, ClientDetailVO clientDetailVO);

    void updateRunTimeDetail(Client client, RuntimeDetailVO runtimeDetailVO);

    void updatePortDetail(Client client, PortDetailVO portDetailVO);

    List<ClientPreviewVO> listClients(int userId,String email);

    List<ClientSimpleVO> listSimpleDetails();

    void renameClient(RenameClientVO renameClientVO);

    ClientDetailsVO getClientDetails(String id);

    void renameNode(RenameNodeVO renameNodeVO);

    RuntimeHistoryVO clientRuntimeDetailsHistory(String id);

    RuntimeDetailVO clientRuntimeDetailsNow(String id);

    PortDetailVO clientPortDetails(String id);

    void deleteClient(String id);
}
