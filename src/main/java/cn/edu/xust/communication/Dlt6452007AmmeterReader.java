package cn.edu.xust.communication;

import cn.edu.xust.communication.enums.AmmeterReader;
import cn.edu.xust.communication.enums.AmmeterStatusEnum;
import cn.edu.xust.communication.protocol.Dlt645Frame;
import cn.edu.xust.communication.server.NettyServer;
import cn.edu.xust.communication.server.handler.NettyServerDefaultHandler;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 支持DLT645-2007协议数据解析的具体实现
 *
 * @author ：huangxin
 * @modified ：
 * @since ：2020/03/19 11:21
 */
public class Dlt6452007AmmeterReader extends AbstractAmmeterReaderWriterAdapter {

    /**
     * 电表端的IP
     */
    private String ammeterChannelIp;
    /**
     * 电表编号
     */
    private String ammeterId;

    private static Queue<String> executedMethodQueue = new ConcurrentLinkedQueue<>();

    public static Queue<String> getExecutedMethodQueue(){
        return executedMethodQueue;
    }

    public Dlt6452007AmmeterReader(String ammeterChannelIp, String ammeterId) {
        this.ammeterChannelIp = ammeterChannelIp;
        this.ammeterId = ammeterId;
        /**
         * 本次命令是否发送成功，成功返回true  失败返回false
         */
        boolean writeCommandSuccess = true;
    }

    @Override
    public void readCurrentVA() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 34 34 35");
    }

    @Override
    public void readCurrentVB() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 35 34 35");
    }

    @Override
    public void readCurrentVC() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 36 34 35");
    }

    @Override
    public void readCurrentIA() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 34 35 35");
    }

    @Override
    public void readCurrentIB() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 35 35 35");
    }

    @Override
    public void readCurrentIC() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 36 35 35");
    }

    @Override
    public void readCurrentActivePower() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 36 35");
    }

    @Override
    public void readCurrentReactivePower() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 37 35");
    }

    @Override
    public void readCurrentPowerFactor() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 39 35");
    }

    @Override
    public void readCurrentTotalApparentPower() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 38 35");
    }


    @Override
    public void readCurrentTotalActiveEnergy() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 33 33");
    }

    @Override
    public void readCurrentPositiveActiveEnergy() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 34 33");
    }

    @Override
    public void readCurrentNegativeActiveEnergy() {
        this.sendCommand(AmmeterReader.MasterRequestFrame.getControlCode(),
                AmmeterReader.MasterRequestFrame.getBaseDataLen(), "33 33 35 33");
    }

    /**
     * 发送指令
     *
     * @param controlCode        控制码
     * @param dataLen            数据长度
     * @param dataIdentification 数据标识
     */
    private void sendCommand(String controlCode, String dataLen, String dataIdentification) {
        if (StringUtils.isEmpty(controlCode) || StringUtils.isEmpty(dataLen) || StringUtils.isEmpty(dataIdentification)
                || StringUtils.isEmpty(this.ammeterId) || StringUtils.isEmpty(this.ammeterChannelIp)) {
            throw new IllegalArgumentException("Message parameter can not be null");
        }
        try {
            Dlt645Frame frame = new Dlt645Frame(this.ammeterId, controlCode, dataLen, dataIdentification);
            NettyServer.writeCommand(this.ammeterChannelIp, frame.createFrame());
            //阻塞线程，等待设备的响应。不要删除！不然会因为服务器发送指令速度太快而设备处理不过来
            Thread.sleep(1000);
        } catch (Exception e) {
            //记录故障
            NettyServerDefaultHandler.logException(e, this.ammeterId, AmmeterStatusEnum.NETWORK_ERROR.getMessage());
        }
    }

    /**
     * 自动调用所有采集方法
     */
    public void start() {
        try {
            Class<? extends Dlt6452007AmmeterReader> clazz = this.getClass();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!"start".equalsIgnoreCase(method.getName()) &&
                        method.getName().startsWith("read")) {
                    method.invoke(this);
                    //将执行过的方法入队
                    executedMethodQueue.offer(method.getName());
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
