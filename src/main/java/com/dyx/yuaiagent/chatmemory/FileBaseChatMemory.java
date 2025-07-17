package com.dyx.yuaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileBaseChatMemory implements ChatMemory {

    private final String BASE_BIR;
    private static final Kryo kryo=new Kryo();
    static {
        kryo.setRegistrationRequired(false);//动态注册实例化的类
        //设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }
    public FileBaseChatMemory(String dir)
    {
        this.BASE_BIR=dir;
        File baseDir = new File(dir);
        if (!baseDir.exists())
        {
            baseDir.mkdir();
        }

    }

    @Override
    public void add(String conversationId, Message message) {
        //ChatMemory.super.add(conversationId, message);
        saveConversation(conversationId,List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId,messageList);

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {

        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream()
                .skip(Math.max(0,messageList.size()-lastN)).toList();
    }

    @Override
    public void clear(String conversationId) {
        File fi = getConversationFile(conversationId);
        if (fi.exists()){
            fi.delete();
        }

    }

    /**
     * 获取或创建会话消息的列表
     * @param conversationId
     * @return
     */
    private List<Message> getOrCreateConversation(String conversationId) {
     File file=getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try(Input input=new Input(new FileInputStream(file))) {
                messages=kryo.readObject(input,ArrayList.class);
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return messages;
    }
    /**
     * 保存会话消息的列表
     * @param conversationId
     * @return
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file=getConversationFile(conversationId);
        try {
            Output output=new Output(new FileOutputStream(file));
            kryo.writeObject(output,messages);
        }catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    private File getConversationFile(String conversationId)
    {
        return new File(BASE_BIR,conversationId+".kryo");
    }

}


