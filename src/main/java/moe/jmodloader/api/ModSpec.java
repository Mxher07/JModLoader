package moe.jmodloader.api;

import java.util.List;

public class ModSpec {
    private String id;
    private String name;
    private String version;
    private String mainClass;
    private String description;
    private String author;
    private String license;
    private String homepage;
    private List<String> dependencies;
    private String apiVersion;
    private String javaVersion;
    private String[] tags;
    private String icon;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
