package circuitsim.components.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Discovers {@link BuiltinComponent}-annotated classes on the classpath.
 * Supports running from expanded class directories (e.g., `-cp out`) and from a runnable JAR.
 */
final class BuiltinComponentDiscovery {
    private static final String COMPONENTS_ROOT = "circuitsim/components/";
    private static final String COMPONENTS_ROOT_NO_SLASH = "circuitsim/components";

    private BuiltinComponentDiscovery() {
    }

    static List<Definition> discover() {
        ClassLoader loader = BuiltinComponentDiscovery.class.getClassLoader();
        Map<String, Definition> byClassName = new HashMap<>();
        try {
            for (String className : findComponentClassNames(loader)) {
                Definition def = tryLoadDefinition(loader, className);
                if (def != null) {
                    byClassName.putIfAbsent(className, def);
                }
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Component discovery failed: " + e.getMessage());
        }
        List<Definition> result = new ArrayList<>(byClassName.values());
        result.sort(Comparator
                .comparingInt((Definition d) -> d.groupOrder)
                .thenComparing(Definition::group)
                .thenComparingInt(d -> d.paletteOrder)
                .thenComparing(Definition::paletteName));
        return result;
    }

    private static List<String> findComponentClassNames(ClassLoader loader) throws IOException, URISyntaxException {
        List<String> result = new ArrayList<>();
        for (String root : List.of(COMPONENTS_ROOT, COMPONENTS_ROOT_NO_SLASH)) {
            Enumeration<URL> roots = loader.getResources(root);
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    result.addAll(findFromDirectory(url));
                } else if ("jar".equals(protocol)) {
                    result.addAll(findFromJar(url));
                }
            }
        }
        return result;
    }

    private static List<String> findFromDirectory(URL url) throws URISyntaxException, IOException {
        Path rootPath = Path.of(url.toURI());
        if (!Files.isDirectory(rootPath)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        try (var stream = Files.walk(rootPath)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                    .forEach(path -> {
                        String relative = rootPath.relativize(path).toString().replace(File.separatorChar, '/');
                        String className = toClassName(COMPONENTS_ROOT + relative);
                        if (className != null) {
                            result.add(className);
                        }
                    });
        }
        return result;
    }

    private static List<String> findFromJar(URL url) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            List<String> result = new ArrayList<>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(COMPONENTS_ROOT) && name.endsWith(".class")) {
                    String className = toClassName(name);
                    if (className != null) {
                        result.add(className);
                    }
                }
            }
            return result;
        }
    }

    private static String toClassName(String resourceName) {
        if (resourceName == null || !resourceName.endsWith(".class")) {
            return null;
        }
        String name = resourceName.substring(0, resourceName.length() - ".class".length());
        if (name.contains("$")) {
            return null;
        }
        return name.replace('/', '.');
    }

    private static Definition tryLoadDefinition(ClassLoader loader, String className) {
        try {
            Class<?> raw = Class.forName(className, false, loader);
            BuiltinComponent info = raw.getAnnotation(BuiltinComponent.class);
            if (info == null) {
                return null;
            }
            if (!CircuitComponent.class.isAssignableFrom(raw)) {
                System.err.println("@BuiltinComponent used on non-component: " + className);
                return null;
            }
            if (Modifier.isAbstract(raw.getModifiers())) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends CircuitComponent> componentClass = (Class<? extends CircuitComponent>) raw;
            Constructor<? extends CircuitComponent> ctor = componentClass.getConstructor(int.class, int.class);
            String type = info.type().isBlank() ? componentClass.getSimpleName() : info.type().trim();
            String paletteName = info.paletteName().isBlank() ? type : info.paletteName().trim();
            List<String> aliases = new ArrayList<>();
            for (String alias : info.aliases()) {
                if (alias != null && !alias.trim().isEmpty()) {
                    aliases.add(alias.trim());
                }
            }
            return new Definition(info.group(), paletteName, type, aliases, info.paletteVisible(),
                    info.groupOrder(), info.paletteOrder(), (x, y) -> {
                        try {
                            return ctor.newInstance(x, y);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException("Failed to instantiate " + componentClass.getName(), e);
                        }
                    });
        } catch (NoSuchMethodException e) {
            System.err.println("Skipping " + className + ": missing (int,int) constructor");
            return null;
        } catch (ReflectiveOperationException e) {
            System.err.println("Skipping " + className + ": " + e.getMessage());
            return null;
        }
    }

    static final class Definition {
        final String group;
        final String paletteName;
        final String type;
        final List<String> aliases;
        final boolean paletteVisible;
        final int groupOrder;
        final int paletteOrder;
        final ComponentRegistry.ComponentFactory factory;

        private Definition(String group, String paletteName, String type, List<String> aliases, boolean paletteVisible,
                           int groupOrder, int paletteOrder, ComponentRegistry.ComponentFactory factory) {
            this.group = group;
            this.paletteName = paletteName;
            this.type = type;
            this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
            this.paletteVisible = paletteVisible;
            this.groupOrder = groupOrder;
            this.paletteOrder = paletteOrder;
            this.factory = factory;
        }

        String group() {
            return group;
        }

        String paletteName() {
            return paletteName;
        }
    }
}
