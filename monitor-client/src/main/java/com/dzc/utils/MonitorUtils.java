package com.dzc.utils;

import com.dzc.entity.BaseDetail;
import com.dzc.entity.PortDetail;
import com.dzc.entity.RuntimeDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.*;

@Slf4j
@Component
public class MonitorUtils {
    private final SystemInfo info = new SystemInfo();
    private final Properties properties = System.getProperties();

    public BaseDetail monitorBaseDetail() {
        OperatingSystem operatingSystem = info.getOperatingSystem();
        HardwareAbstractionLayer hardware = info.getHardware();
        double memory = hardware.getMemory().getTotal() / 1024.0 / 1024 / 1024;
        double diskSize = Arrays.stream(File.listRoots()).mapToLong(File::getTotalSpace).sum() / 1024.0 / 1024 / 1024;
        String ip = Objects.requireNonNull(this.findNetWorkInterface(hardware)).getIPv4addr()[0];
        return new BaseDetail()
                .setOsArch(properties.getProperty("os.arch"))
                .setOsName(operatingSystem.getFamily())
                .setOsVersion(operatingSystem.getVersionInfo().getVersion())
                .setOsBit(operatingSystem.getBitness())
                .setCpuName(hardware.getProcessor().getProcessorIdentifier().getName())
                .setCpuCore(hardware.getProcessor().getLogicalProcessorCount())
                .setMemory(memory)
                .setDisk(diskSize)
                .setIp(ip);
    }

    public RuntimeDetail monitorRuntimeDetail() {
        double statisticTime = 0.5;
        try {
            HardwareAbstractionLayer hardware = info.getHardware();
            NetworkIF networkInterface = Objects.requireNonNull(this.findNetWorkInterface(hardware));
            CentralProcessor processor = hardware.getProcessor();
            double upload = networkInterface.getBytesSent(), download = networkInterface.getBytesRecv();
            double read = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum();
            double write = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum();
            long[] ticks = processor.getSystemCpuLoadTicks();
            Thread.sleep((long) (statisticTime * 1000));

            networkInterface = Objects.requireNonNull(this.findNetWorkInterface(hardware));
            upload = (networkInterface.getBytesSent() - upload) / statisticTime;
            download = (networkInterface.getBytesRecv() - download) / statisticTime;
            read = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum() - read) / statisticTime;
            write = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum() - write) / statisticTime;

            double memory = (hardware.getMemory().getTotal() - hardware.getMemory().getAvailable()) / 1024.0 / 1024 / 1024;
            double disk = Arrays.stream(File.listRoots())
                    .mapToLong(file -> file.getTotalSpace() - file.getFreeSpace()).sum() / 1024.0 / 1024 / 1024;
            return new RuntimeDetail()
                    .setCpuUsage(this.calculateCpuUsage(processor, ticks))
                    .setMemoryUsage(memory)
                    .setDiskUsage(disk)
                    .setNetworkUpload(upload / 1024)
                    .setNetworkDownload(download / 1024)
                    .setDiskRead(read / 1024 / 1024)
                    .setDiskWrite(write / 1024 / 1024)
                    .setTimestamp(new Date().getTime());
        } catch (InterruptedException e) {
            log.error("读取运行时数据出现问题", e);
        }
        return null;
    }

    public PortDetail monitorPortStatus(){
        return new PortDetail()
                .setHttpStatus(getPortStatus(80))
                .setHttpsStatus(getPortStatus(443))
                .setFtpStatus(getPortStatus(21))
                .setSftpStatus(getPortStatus(22))
                .setFtpsStatus(getPortStatus(990))
                .setSmtpStatus(getPortStatus(25))
                .setTimestamp(new Date().getTime());
    }

    public boolean getPortStatus(int port) {
        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),port),2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private double calculateCpuUsage(CentralProcessor processor, long[] prevTicks) {
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softIrq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long cUser = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long ioWait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = cUser + nice + cSys + idle + ioWait + irq + softIrq + steal;
        return (cSys + cUser) * 1.0 / totalCpu;
    }

    private NetworkIF findNetWorkInterface(HardwareAbstractionLayer hardware) {
        try {
            for (NetworkIF network : hardware.getNetworkIFs()) {
                String[] ipv4Addr = network.getIPv4addr();
                NetworkInterface ni = network.queryNetworkInterface();
                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();
                if (!ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                        && !name.contains("vmware") && !name.contains("virtual")
                        && !displayName.contains("vmware") && !displayName.contains("virtual")
                        && (ni.getName().startsWith("eth") || ni.getName().startsWith("en") || ni.getName().startsWith("wlan"))
                        && ipv4Addr.length > 0) {
                    return network;
                }
            }
        } catch (IOException e) {
            log.error("读取网络接口信息时出错", e);
        }
        return null;
    }
}
