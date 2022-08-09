package com.zxyw.sdk.tools;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by CHENYAN on 2019/5/12.
 */

public class FTPUtils {

    private FtpServer server;
    private static FTPUtils instance;
    private String userName = "admin";
    private String password = "123456";
    private int port;
    private String path;
    private FtpListener listener;

    private FTPUtils() {

    }

    public void init(@NonNull String userName, @NonNull String password, int port, @NonNull String path, @Nullable FtpListener listener) {
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.path = path;
        this.listener = listener;
    }

    public static FTPUtils getInstance() {
        if (instance == null) {
            synchronized (FTPUtils.class) {
                if (instance == null) {
                    instance = new FTPUtils();
                }
            }
        }
        return instance;
    }

    public void startFtp() {
        try {
            FtpServerFactory serverFactory = new FtpServerFactory();
            //设置访问用户名和密码还有共享路径
            BaseUser baseUser = new BaseUser();
            baseUser.setName(userName);
            baseUser.setPassword(password);
            baseUser.setHomeDirectory(path);

            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            baseUser.setAuthorities(authorities);

            serverFactory.getUserManager().save(baseUser);

            ListenerFactory factory = new ListenerFactory();
            factory.setPort(port); //设置端口号 非ROOT不可使用1024以下的端口
            serverFactory.addListener("default", factory.createListener());

            Map<String, Ftplet> ftplets = new HashMap<>();
            ftplets.put("miaFtplet", new MyFtpLet());
            serverFactory.setFtplets(ftplets);

            server = serverFactory.createServer();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopFtp() {
        if (server != null) {
            server.stop();
        }
    }

    class MyFtpLet extends DefaultFtplet {

        @Override
        public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
            return super.beforeCommand(session, request);
        }

        @Override
        public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
            return super.afterCommand(session, request, reply);
        }

        @Override
        public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
            return super.onUploadStart(session, request);
        }

        @Override
        public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
            String argument = request.getArgument();
            if (argument == null) {
                return super.onUploadEnd(session, request);
            }
            if (listener != null) listener.onReceiveFile(argument);
            return super.onUploadEnd(session, request);
        }
    }
}
