# Spring Boot 自动配置(autoconfigure)原理

## 0. 说明

环境配置清单

> java version "1.8.0_161"

> Java(TM) SE Runtime Environment (build 1.8.0_161-b12)

> Java HotSpot(TM) 64-Bit Server VM (build 25.161-b12, mixed mode)

> Spring Boot  2.1.0.RELEASE

项目 [GitHub](https://github.com/sunlightzy/spring-boot-auto-configuration)

## 1. 前提知识

### 一、SPI扩展机制

#### 1. 解释

SPI:  `Service Provider Interface` , 即 服务提供接口

#### 2. 如何写一个Java SPI呢?

1. 定义一组接口， 接口是 `com.glmapper.spi.FilterProvider`；
2. 接口的一个或多个实现(`com.glmapper.spi.provider.FileFilterProvider` [从文件系统加载filter], `com.glmapper.spi.provider.DataSourceFilterProvider` [从数据源中加载filter])；
3. 在 `src/main/resources/` 下建立 `/META-INF/services` 目录， 新增一个以接口命名的文件 `com.glmapper.spi.FilterProvider`, 内容是要对应的实现类(`com.glmapper.spi.provider.FileFilterProvider` 或 `com.glmapper.spi.provider.DataSourceFilterProvider` 或两者)；
4. 使用 `ServiceLoader` 来加载配置文件中指定的实现。

#### 3. SPI应用案例

1. `Dubbo` 中有大量的`SPI`应用,不过`Dubbo`不是原生的`java spi`机制,他是原生的一个变种 . `Dubbo SPI`  约定:

   1. 扩展点约定 :  扩展点必须是 `Interface` 类型 ， 必须被 `@SPI` 注解 ， 满足这两点才是一个扩展点。
   2. 扩展定义约定 ： 在 `META-INF/services/、META-INF/dubbo/、META-INF/dubbo/internal/`目录下新建扩展点文件,这些路径下定义的文件名称为扩展点接口的全类名 , 文件中以键值对的方式配置扩展点的扩展实现。例如文件  `META-INF/dubbo/internal/com.alibaba.dubbo.common.extension.ExtensionFactory`  中定义的扩展 ：

   ```properties
   adaptive=com.alibaba.dubbo.common.extension.factory.AdaptiveExtensionFactory
   spi=com.alibaba.dubbo.common.extension.factory.SpiExtensionFactory
   spring=com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory
   ```

   关于`Dubbo SPI`扩展机制在此不再继续展开描述

2. `JDBC` 数据库驱动包: `java mysql` 驱动采用原生的`spi`机制`mysql-connector-java-xxx.jar` 就有一个 `/META-INF/services/java.sql.Driver` 里面内容是 

```properties
com.mysql.jdbc.Driver
com.mysql.fabric.jdbc.FabricMySQLDriver
```

3. 当然还有今天的主角 `spring boot` ,他也是原生`spi`的变种,它的约定是在`src/main/resorces/`下建立`META-INF/spring.factories`, 当springboot服务启动时，对象实例化过程会加载`META-INF/spring.factories`文件，将该配置文件中的配置的类载入到Spring容器中.下面是`spring-boot-autoconfigure jar`包中`spring.factories` 的内容:

```properties
# Initializers
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
org.springframework.boot.autoconfigure.logging.AutoConfigurationReportLoggingInitializer

# Application Listeners
org.springframework.context.ApplicationListener=\
org.springframework.boot.autoconfigure.BackgroundPreinitializer

# Auto Configuration Import Listeners
org.springframework.boot.autoconfigure.AutoConfigurationImportListener=\
org.springframework.boot.autoconfigure.condition.ConditionEvaluationReportAutoConfigurationImportListener

# Auto Configuration Import Filters
org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
org.springframework.boot.autoconfigure.condition.OnClassCondition

# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,\
org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration,\
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration,\
org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration,\
org.springframework.boot.autoconfigure.web.HttpEncodingAutoConfiguration,\
org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration,\
org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration,\
org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration,\
org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration,\
org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration,\
# 这里省略了一堆

# Failure analyzers
org.springframework.boot.diagnostics.FailureAnalyzer=\
org.springframework.boot.autoconfigure.diagnostics.analyzer.NoSuchBeanDefinitionFailureAnalyzer,\
org.springframework.boot.autoconfigure.jdbc.DataSourceBeanCreationFailureAnalyzer,\
org.springframework.boot.autoconfigure.jdbc.HikariDriverConfigurationFailureAnalyzer

# Template availability providers
org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider=\
org.springframework.boot.autoconfigure.freemarker.FreeMarkerTemplateAvailabilityProvider,\
org.springframework.boot.autoconfigure.mustache.MustacheTemplateAvailabilityProvider,\
org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAvailabilityProvider,\
org.springframework.boot.autoconfigure.thymeleaf.ThymeleafTemplateAvailabilityProvider,\
org.springframework.boot.autoconfigure.web.JspTemplateAvailabilityProvider

```

## 2. Spring Boot 自动配置机制

### 0. 总体流程概述



### 1. 几个重要的事件回调机制

#### ApplicationContextInitializer

配置在`META-INF/spring.factories`

```properties
# Initializers
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
org.springframework.boot.autoconfigure.logging.AutoConfigurationReportLoggingInitializer
```

**ApplicationContextInitializer 是上下文初始化入口**

![image-20181125164451526](https://ws4.sinaimg.cn/large/006tNbRwly1fxkeicqfdrj317y0u07f2.jpg)



#### SpringApplicationRunListener

配置在`META-INF/spring.factories`

```properties
# Application Listeners
org.springframework.context.ApplicationListener=\
org.springframework.boot.autoconfigure.BackgroundPreinitializer
```

SpringApplicationRunListener 的功能是监听容器启动过程也就是`SpringApplication.run()`方法,

![image-20181125164340483](https://ws3.sinaimg.cn/large/006tNbRwly1fxkeh4v699j31c40qqteo.jpg)



#### ApplicationRunner & CommandLineRunner

`CommandLineRunner & ApplicationRunner` 接口是在容器启动成功后的最后一步回调（类似开机自启动）, 两者功能差不多, 只需要将其实现类放在`IOC`容器中,应用启动后会自动回调接口方法

![image-20181125164546033](https://ws4.sinaimg.cn/large/006tNbRwly1fxkeja1e7aj31bo0mktdo.jpg)

![image-20181125164642736](https://ws3.sinaimg.cn/large/006tNbRwly1fxkek96kk9j31b40ootfu.jpg)

### 2. 自动配置注解

#### @EnableAutoConfiguration

**@EnableAutoConfiguration是自动配置的开关, 下面看看他的结构**

```java
/**
 * Enable auto-configuration of the Spring Application Context, attempting to guess and
 * configure beans that you are likely to need. Auto-configuration classes are usually
 * applied based on your classpath and what beans you have defined. For example, if you
 * have {@code tomcat-embedded.jar} on your classpath you are likely to want a
 * {@link TomcatServletWebServerFactory} (unless you have defined your own
 * {@link ServletWebServerFactory} bean).
 * <p>
 * When using {@link SpringBootApplication}, the auto-configuration of the context is
 * automatically enabled and adding this annotation has therefore no additional effect.
 * <p>
 * Auto-configuration tries to be as intelligent as possible and will back-away as you
 * define more of your own configuration. You can always manually {@link #exclude()} any
 * configuration that you never want to apply (use {@link #excludeName()} if you don't
 * have access to them). You can also exclude them via the
 * {@code spring.autoconfigure.exclude} property. Auto-configuration is always applied
 * after user-defined beans have been registered.
 * <p>
 * The package of the class that is annotated with {@code @EnableAutoConfiguration},
 * usually via {@code @SpringBootApplication}, has specific significance and is often used
 * as a 'default'. For example, it will be used when scanning for {@code @Entity} classes.
 * It is generally recommended that you place {@code @EnableAutoConfiguration} (if you're
 * not using {@code @SpringBootApplication}) in a root package so that all sub-packages
 * and classes can be searched.
 * <p>
 * Auto-configuration classes are regular Spring {@link Configuration} beans. They are
 * located using the {@link SpringFactoriesLoader} mechanism (keyed against this class).
 * Generally auto-configuration beans are {@link Conditional @Conditional} beans (most
 * often using {@link ConditionalOnClass @ConditionalOnClass} and
 * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnClass
 * @see AutoConfigureAfter
 * @see SpringBootApplication
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
// 自动配置包,注册扫描包
@AutoConfigurationPackage
// 导入的这个AutoConfigurationImportSelector是自动配置的关键
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {

	String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

	/**
	 * Exclude specific auto-configuration classes such that they will never be applied.
	 * @return the classes to exclude
	 */
	Class<?>[] exclude() default {};

	/**
	 * Exclude specific auto-configuration class names such that they will never be
	 * applied.
	 * @return the class names to exclude
	 * @since 1.3.0
	 */
	String[] excludeName() default {};

}
```

进入`AutoConfigurationImportSelector`找到`selectImports()`方法，他调用了`getCandidateConfigurations()`方法，在这里，这个方法又调用了`Spring Core`包中的`loadFactoryNames()`方法。这个方法的作用是，会查询`META-INF/spring.factories`文件中包含的`JAR`文件。

```java
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        if (!isEnabled(annotationMetadata)) {
            return NO_IMPORTS;
        }
        //1. 得到注解信息
        AutoConfigurationMetadata autoConfigurationMetadata = AutoConfigurationMetadataLoader
            .loadMetadata(this.beanClassLoader);
        AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(
            autoConfigurationMetadata, annotationMetadata);
        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }

	/**
	 * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
	 * of the importing {@link Configuration @Configuration} class.
	 * @param autoConfigurationMetadata the auto-configuration metadata
	 * @param annotationMetadata the annotation metadata of the configuration class
	 * @return the auto-configurations that should be imported
	 */
	protected AutoConfigurationEntry getAutoConfigurationEntry(
			AutoConfigurationMetadata autoConfigurationMetadata,
			AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return EMPTY_ENTRY;
		}
        // 2. 得到注解中的所有属性信息
		AnnotationAttributes attributes = getAttributes(annotationMetadata);
        // 3. 得到spring.factories中配置在EnableAutoConfiguration下的字符串列表
		List<String> configurations = getCandidateConfigurations(annotationMetadata,
				attributes);
        // 4. 去重
		configurations = removeDuplicates(configurations);
        // 5. 根据注解中的exclude信息去除不需要的
		Set<String> exclusions = getExclusions(annotationMetadata, attributes);
		checkExcludedClasses(configurations, exclusions);
		configurations.removeAll(exclusions);
		configurations = filter(configurations, autoConfigurationMetadata);
        // 7. 派发事件
		fireAutoConfigurationImportEvents(configurations, exclusions);
		return new AutoConfigurationEntry(configurations, exclusions);
	}

	/**
	 * 获取所有的自动配置类,也就是配置在spring.factories中 EnableAutoConfiguration 下的所有字符串列表
	 * Return the auto-configuration class names that should be considered. By default
	 * this method will load candidates using {@link SpringFactoriesLoader} with
	 * {@link #getSpringFactoriesLoaderFactoryClass()}.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return a list of candidate configurations
	 */
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
        // getSpringFactoriesLoaderFactoryClass()直接返回EnableAutoConfiguration.class
        // 所以这一步加载了所有的自动配置类
		List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
				getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader());
		Assert.notEmpty(configurations,
				"No auto configuration classes found in META-INF/spring.factories. If you "
						+ "are using a custom packaging, make sure that file is correct.");
		return configurations;
	}

	/**
	 * Return the class used by {@link SpringFactoriesLoader} to load configuration
	 * candidates.
	 * @return the factory class
	 */
	protected Class<?> getSpringFactoriesLoaderFactoryClass() {
		return EnableAutoConfiguration.class;
	}
```

下面进入`org.springframework.core.io.support.SpringFactoriesLoader#loadFactoryNames`

```java
/**
 * Load the fully qualified class names of factory implementations of the
 * given type from {@value #FACTORIES_RESOURCE_LOCATION}, using the given
 * class loader.
 * @param factoryClass the interface or abstract class representing the factory
 * @param classLoader the ClassLoader to use for loading resources; can be
 * {@code null} to use the default
 * @throws IllegalArgumentException if an error occurs while loading factory names
 * @see #loadFactories
 */
public static List<String> loadFactoryNames(Class<?> factoryClass, @Nullable ClassLoader classLoader) {
    // 获取全类名
   String factoryClassName = factoryClass.getName();
    // 加载所有的spring.factories中的配置,然后筛选出factoryClassName下的配置的值
   return loadSpringFactories(classLoader).getOrDefault(factoryClassName, Collections.emptyList());
}
```

![image-20181125170438713](https://ws3.sinaimg.cn/large/006tNbRwly1fxkfaf053aj31ac0u0h0m.jpg)

在上面的`spring-boot-autoconfigure.jar`里的`spring.factories`文件下我们可以看到有这么一段关于`EnableAutoConfiguration`的配置(放一小段)

```properties
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration,\
org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration,\
org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.web.HttpEncodingAutoConfiguration,\
org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration,\
org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration,\
org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration,\
org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration,\
org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration,\
org.springframework.boot.autoconfigure.websocket.WebSocketMessagingAutoConfiguration,\
org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration
```

在`SpringBoot`启动配置类上面打上`@EnableAutoConfiguration`注解之后`springboot`就会实例化配置文件中这些`XxxAutoConfiguration`类启用这些类的功能, 

==**需要注意的是**: 加了`@EableAutoConfiguration`注解的配置类只会为这个类所在的包以及子包下面的类自动配置==

`@EnableAutoConfiguration`是自动配置的开关 ,如果要自己写自动配置类,还有一些`Conditional`的注解类需要掌握

#### @ConditionalOnXxx系列注解

**`SpringBoot`的自动配置全都依赖于这个系列的注解,下面列举了一些:**

> `ConditionalOnBean` 				当指定bean存在时, 配置生效
> `ConditionalOnClass` 				当指定类存在时, 配置生效
> `ConditionalOnCloudPlatform ` 		当项目环境为指定云平台环境时, 配置生效
> `ConditionalOnEnableResourceChain` 	当`ResourceChain`是启用状态时, 配置生效
> `ConditionalOnExpression`			当表达式为true时, 配置生效
> `ConditionalOnJava`				当环境的java为指定版本时,配置生效
> `ConditionalOnJndi`				当指定的`JNDI`存在时, 配置生效
> `ConditionalOnMissingBean`			当指定的bean不存在时, 配置生效
> `ConditionalOnMissingClass`		当指定的类不存在时, 配置生效
> `ConditionalOnNotWebApplication`	当项目为非web项目时, 配置生效
> `ConditionalOnProperty`			当指定的配置存在时, 配置生效
> `ConditionalOnResource`			当指定的资源存在时, 配置生效
> `ConditionalOnSingleCandidate`		当指定的类是单例时, 配置生效
> `ConditionalOnWebApplication`		当项目是web项目时, 配置生效


![image-20181125172244614](https://ws2.sinaimg.cn/large/006tNbRwly1fxkflre2kcj327u0motr4.jpg)

#### 举个栗子

下面以`HttpEncodingAutoConfiguration`为例来看一下自动配置

> `@ConditionalOnProperty`注解的玩法很多, 详细使用案例参考本文文末附件**@ConditionalOnProperty注解**

```java
/**
 * {@link EnableAutoConfiguration Auto-configuration} for configuring the encoding to use
 * in web applications.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 1.2.0
 */
@Configuration
// 启用HttpProperties配置并加入到IOC容器
@EnableConfigurationProperties(HttpProperties.class)
// 当项目是servlet容器下的web项目时,这个配置类才生效
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
// 当CharacterEncodingFilter类存在时,这个配置类才生效
@ConditionalOnClass(CharacterEncodingFilter.class)
// 当spring.http.encoding.enabled这个环境变量存在且值不为false时,这个配置类才生效
// @ConditionalOnProperty这个注解的玩法很多
@ConditionalOnProperty(prefix = "spring.http.encoding", value = "enabled", matchIfMissing = true)
public class HttpEncodingAutoConfiguration {

   private final HttpProperties.Encoding properties;

   public HttpEncodingAutoConfiguration(HttpProperties properties) {
      this.properties = properties.getEncoding();
   }

   @Bean
   // 当容器中没有CharacterEncodingFilter类型的实例时,这个方法生效
   @ConditionalOnMissingBean
   public CharacterEncodingFilter characterEncodingFilter() {
      CharacterEncodingFilter filter = new OrderedCharacterEncodingFilter();
      filter.setEncoding(this.properties.getCharset().name());
      filter.setForceRequestEncoding(this.properties.shouldForce(Type.REQUEST));
      filter.setForceResponseEncoding(this.properties.shouldForce(Type.RESPONSE));
      return filter;
   }

   @Bean
   public LocaleCharsetMappingsCustomizer localeCharsetMappingsCustomizer() {
      return new LocaleCharsetMappingsCustomizer(this.properties);
   }

   private static class LocaleCharsetMappingsCustomizer implements
         WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

      private final HttpProperties.Encoding properties;

      LocaleCharsetMappingsCustomizer(HttpProperties.Encoding properties) {
         this.properties = properties;
      }

      @Override
      public void customize(ConfigurableServletWebServerFactory factory) {
         if (this.properties.getMapping() != null) {
            factory.setLocaleCharsetMappings(this.properties.getMapping());
         }
      }

      @Override
      public int getOrder() {
         return 0;
      }
   }
}
```

## 3. SpringBoot启动过程

### 0. 总体流程概述

### 1. 创建启动类

1.1. 创建启动类

```java
@SpringBootApplication
public class Bootstrap {
    public static void main(String[] args) {
        // 调用SpringApplication静态方法run为入口
        SpringApplication.run(Bootstrap.class, args);
    }
}
```

1.2. 跟踪进入`org.springframework.boot.SpringApplication#run(java.lang.String...)`

```java
    /**
     * Static helper that can be used to run a {@link SpringApplication} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
        // 进入构造器,会有初始化过程
       return new SpringApplication(sources).run(args);
    }
	/**
	 * Create a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified sources (see {@link SpringApplication class-level}
	 * documentation for details. The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param sources the bean sources
	 * @see #run(Object, String[])
	 * @see #SpringApplication(ResourceLoader, Object...)
	 */
	public SpringApplication(Object... sources) {
        // 初始化
		initialize(sources);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize(Object[] sources) {
		if (sources != null && sources.length > 0) {
			this.sources.addAll(Arrays.asList(sources));
		}
        // 推断是否为web环境
		this.webEnvironment = deduceWebEnvironment();
        // 获取所有的配置在spring.factores中的ApplicationContextInitializer
		setInitializers((Collection) getSpringFactoriesInstances(
				ApplicationContextInitializer.class));
        // 获取所有的配置在spring.factores中的ApplicationContextInitializer
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
        // 推断启动类,我们这里就是Bootstrap.class
		this.mainApplicationClass = deduceMainApplicationClass();
	}
```



```java
	/**
	 * 运行spring应用,创建一个新的spring上ApplicationContext下文环境
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		FailureAnalyzers analyzers = null;
        // 加载java的AWT图形化相关的系统配置变量, 可以忽略
		configureHeadlessProperty();
        // 实例化spring.factories中配置的所有SpringApplicationRunListener并返回到 listeners 中
        // SpringApplicationRunListeners内部是一个List<SpringApplicationRunListener>
		SpringApplicationRunListeners listeners = getRunListeners(args);
        // 启动应用监听器,回调starting方法,
        // spring应用进行到某一阶段时会广播通知所有的监听器, 监听器的方法就会被回调执行
		listeners.starting();
		try {
            // 包装命令行启动参数 也就是 Bootstrap.main(String[] args)中的args
            // 我们可以通过命令号启动应用 java -jar demo.jar --server.port=8989 这个server.port=8989就是启动参数
            // 他可以接受多个启动参数,包括指定profile [dev/test/pre/prod]
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
            // 准备应用环境, 包括读取系统环境变量/yml,properties等配置文件, 
            // 同时回调listeners的environmentPrepared方法
			ConfigurableEnvironment environment = prepareEnvironment(listeners,
					applicationArguments);
            // 打印Banner 也就是我们启动应用时控制台打印出的Spring 的 logo了,这个也可以自定义
            // 有兴趣的自行百度自定义springboot banner, 我不喜欢这些花里胡哨的东西(才怪)
			Banner printedBanner = printBanner(environment);
            // 创建上下文,决定创建web的ioc还是普通的ioc
			context = createApplicationContext();
            // 实例化配置在spring.factories中的FailureAnalyzer应用启动失败的分析器,并返回
			analyzers = new FailureAnalyzers(context);
            // 上下文准备,会广播通知listeners回调contextPrepared方法
			prepareContext(context, environment, listeners, applicationArguments,
					printedBanner);
            // 刷新上下文
			refreshContext(context);
            // 上下文刷新后的一些擦屁股工作
			afterRefresh(context, applicationArguments);
            // 容器已经创建和刷新完成,广播通知listeners回调finished方法
			listeners.finished(context, null);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass)
						.logStarted(getApplicationLog(), stopWatch);
			}
            // 到此如果没有启动报错,那你的应用就已经启动完成了
			return context;
		}
		catch (Throwable ex) {
			handleRunFailure(context, listeners, analyzers, ex);
			throw new IllegalStateException(ex);
		}
	}
```

我们要说的是`springboot`自动配置, 但是我写这些做什么呢? 因为自动配置就是在上面的一些步骤中完成的,下面继续

总结一下,应用启动过程经历了哪些阶段呢. 

1. `getRunListeners(...)`获取SpringApplicationRunListener监听器
2. `prepareEnvironment(...)`应用环境准备
3. `createApplicationContext(...)`创建应用上下文
4. `prepareContext(...)`上下文准备
5. `refreshContext(...)`刷新上下文
6. `afterRefresh(...)`上下文刷新完后的一些收尾工作



### 2. prepareEnvironment

**容器环境准备阶段**

```java
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
                                                   ApplicationArguments applicationArguments) {
    // 存在就获取环境,不存在就创建环境
    ConfigurableEnvironment environment = getOrCreateEnvironment();
    // 环境配置:
    // 1. 收集用户自定义的配置和系统环境变量
    // 2. 收集Profiles信息
    configureEnvironment(environment, applicationArguments.getSourceArgs());
    listeners.environmentPrepared(environment);
    if (!this.webEnvironment) {
        environment = new EnvironmentConverter(getClassLoader())
            .convertToStandardEnvironmentIfNecessary(environment);
    }
    return environment;
}
```



### 3. createApplicationContext

**根据是否为web环境来决定创建一个web应用或者非web应用的上下文**

```java
/**
 * Strategy method used to create the {@link ApplicationContext}. By default this
 * method will respect any explicitly set application context or application context
 * class before falling back to a suitable default.
 * @return the application context (not yet refreshed)
 * @see #setApplicationContextClass(Class)
 */
protected ConfigurableApplicationContext createApplicationContext() {
   Class<?> contextClass = this.applicationContextClass;
   if (contextClass == null) {
      try {
         contextClass = Class.forName(this.webEnvironment
               ? DEFAULT_WEB_CONTEXT_CLASS : DEFAULT_CONTEXT_CLASS);
      }
      catch (ClassNotFoundException ex) {
         throw new IllegalStateException(
               "Unable create a default ApplicationContext, "
                     + "please specify an ApplicationContextClass",
               ex);
      }
   }
   return (ConfigurableApplicationContext) BeanUtils.instantiate(contextClass);
}
```



### 4. prepareContext

**上下文的一些成员变量初始化工作**

```java
private void prepareContext(ConfigurableApplicationContext context,
      ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
      ApplicationArguments applicationArguments, Banner printedBanner) {
   context.setEnvironment(environment);
   postProcessApplicationContext(context);
    // 划重点了, 初始化开始了
   applyInitializers(context);
   listeners.contextPrepared(context);
   if (this.logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
   }

   // Add boot specific singleton beans
   context.getBeanFactory().registerSingleton("springApplicationArguments",
         applicationArguments);
   if (printedBanner != null) {
      context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);
   }

   // Load the sources
   Set<Object> sources = getSources();
   Assert.notEmpty(sources, "Sources must not be empty");
   load(context, sources.toArray(new Object[sources.size()]));
   listeners.contextLoaded(context);
}
```

==**重点开始**==

```java
/**
 * Apply any {@link ApplicationContextInitializer}s to the context before it is
 * refreshed.
 * @param context the configured ApplicationContext (not refreshed yet)
 * @see ConfigurableApplicationContext#refresh()
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
protected void applyInitializers(ConfigurableApplicationContext context) {
   for (ApplicationContextInitializer initializer : getInitializers()) {
      Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(
            initializer.getClass(), ApplicationContextInitializer.class);
      Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
      initializer.initialize(context);
   }
}
```

### 5. refreshContext





### 6. afterRefresh





## 4. 附件

##### @ConditionalOnProperty注解

###### 一、@ConditionalOnProperty 结构
```java
@Retention(RetentionPolicy.RUNTIME)  
@Target({ElementType.TYPE, ElementType.METHOD})  
@Documented  
@Conditional({OnPropertyCondition.class})  
public @interface ConditionalOnProperty {  
    //数组，获取对应property名称的值，不可与name同时使用  
    String[] value() default {};
  
    //property名称的前缀，可有可无
    String prefix() default "";  
  
    //数组，property完整名称或部分名称（可与prefix组合使用，组成完整的property名称），不可与value同时使用 
    String[] name() default {}; 
  
    //可与name组合使用，比较获取到的属性值与havingValue给定的值是否相同，相同才加载配置  
    String havingValue() default "";
  
    //缺少该property时是否可以加载。如果为true，没有该property也会正常加载；反之报错
    boolean matchIfMissing() default false;  
  
    //是否可以松散匹配  
    boolean relaxedNames() default true;
} 
```

###### 二、@ConditionalOnProperty 用法

**1. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(value = "object.pool.size")  
public class ObjectPoolConfig { 

}
```
yml配置如下：
```yml
object.pool:  
    size: true     //正常  
object.pool:  
    size:          //正常，空字符时   
object.pool:  
    size: false    //失败  
object.pool:  
    size: null     //正常  
object.pool:  
    size: 30       //正常 
```
**2. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(value = "object.pool.size",havingValue="30")  
public class ObjectPoolConfig { 

}
```
yml配置如下：
```yml
object.pool:  
    size: 1234     //失败,与havingValue给定的值不一致     
object.pool:  
    size: false    //失败,与havingValue给定的值不一致  
object.pool:  
    size: 30       //正常 
```
> 当且仅当配置文件中的Value和havingValue的值一致时才加载成功
**3. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(prefix = "object.pool",name = "size",havingValue="30")  
public class ObjectPoolConfig { 

}
```
yml配置如下：
```yml
object.pool:  
    size: 1234     //失败,与havingValue给定的值不一致
object.pool:  
    size: false    //失败,与havingValue给定的值不一致
object.pool:  
    size: 30       //正常 
```
**4. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(prefix = "object.pool",name = "size",havingValue="30",matchIfMissing = true)  
public class ObjectPoolConfig { 

}
```
> yml不配置相关参数,正常启动，当 matchIfMissing = true 时，即使没有 `object.pool.size` 属性也会加载正常 
**5. 有如下spring boot代码和yml配置**

```java
@Configuration  
//matchIfMissing的缺省值为false
@ConditionalOnProperty(prefix = "object.pool",name = "size",havingValue="30",matchIfMissing = false)  
public class ObjectPoolConfig { 

}
```
yml配置如下：
> yml不配置相关参数,加载失败,当 matchIfMissing = false 时，必须要有对应的属性配置
```yml
object.pool:  
    size: 1234     //失败,与havingValue给定的值不一致
object.pool:  
    size: false    //失败,与havingValue给定的值不一致
object.pool:  
    size: 30       //正常 
```
**6. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(prefix = "object.pool",name = {"size","timeout"})  //name中的属性需要两个都存在且都不为false才会加载正常  
public class ObjectPoolConfig { 

}
```
yml配置如下：
```yml
object.pool: 
    timeout: true
    size: 1234     //正常
object.pool:  
    timeout: true
    size: false    //失败,两个值都不能为 false
object.pool:  
    timeout: true
    size: true     //正常 
```
**7. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(prefix = "object.pool",name = {"size","timeout"},havingValue="false") 
public class ObjectPoolConfig { 

}
```
.yml配置如下：
```yml
object.pool:  
    timeout: false
    size: false    //正常
```
**8. 有如下spring boot代码和yml配置**

```java
@Configuration  
@ConditionalOnProperty(prefix = "object.pool", name = {"size", "timeout"}, havingValue = "100", matchIfMissing = true) 
public class ObjectPoolConfig { 

}
```
yml配置如下：
```yml
object.pool:  
    timeout: 123
    size: false     //失败,和havingValue的值不一致
object.pool:  
    timeout: 123
    size: 1234      //失败,和havingValue的值不一致
object.pool:  
    timeout: 123
    size: 123       //正常
```
> matchIfMissing = true , 不配置参数也正常
###### 三、 @ConditionalOnProperty 应用场景
1. 通过 `@ConditionalOnProperty` 来控制 `Configuration` 是否生效