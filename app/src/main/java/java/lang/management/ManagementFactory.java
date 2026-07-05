package java.lang.management;

public class ManagementFactory {
    public static MemoryMXBean getMemoryMXBean() {
        return new MemoryMXBean() {
            public MemoryUsage getHeapMemoryUsage() {
                return new MemoryUsage(0, Runtime.getRuntime().maxMemory(), Runtime.getRuntime().maxMemory(), 0);
            }
        };
    }
}
