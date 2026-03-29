package moe.jmodloader.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 模组类加载器，实现模组间的类隔离
 * ModClassLoader for isolating classes between different mods
 */
public class ModClassLoader extends URLClassLoader {
    private final String modId;
    private final Map<String, ModClassLoader> dependencyLoaders = new HashMap<>();

    public ModClassLoader(String modId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.modId = modId;
    }

    public void addDependencyLoader(String id, ModClassLoader loader) {
        dependencyLoaders.put(id, loader);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. 尝试从依赖的模组中加载类
        // Try loading class from dependent mods
        for (ModClassLoader loader : dependencyLoaders.values()) {
            try {
                return loader.loadClassFromThis(name);
            } catch (ClassNotFoundException ignored) {}
        }
        
        // 2. 尝试从当前模组加载类
        // Try loading class from current mod
        return super.findClass(name);
    }

    /**
     * 仅从当前加载器中查找类，不委托给父类或依赖
     * Find class only in this loader, without delegating to parent or dependencies
     */
    public Class<?> loadClassFromThis(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name);
            }
            return c;
        }
    }

    public String getModId() {
        return modId;
    }
}
