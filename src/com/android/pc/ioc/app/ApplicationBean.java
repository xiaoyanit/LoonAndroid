package com.android.pc.ioc.app;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import com.android.pc.ioc.core.kernel.KernelObject;
import com.android.pc.ioc.core.kernel.KernelReflect;
import com.android.pc.ioc.db.sqlite.DbUtils;
import com.android.pc.ioc.util.ContextUtils;
import com.android.pc.ioc.util.InjectViewUtils;
import com.android.pc.util.Handler_Properties;
import com.android.pc.util.Logger;

public class ApplicationBean extends Application {

	/**
	 * Application对象
	 */
	private static ApplicationBean Application;
	public static Logger logger  = null;
	/**
	 * 默认高度和宽度,所有的缩放比根据这个常数来获得
	 */
	private int mode_w = 480;
	private int mode_h = 800;
	private InstrumentationBean instrumentation;
	private HashMap<String, DbUtils> dbMap = new HashMap<String, DbUtils>();
	private String dbName = "db";
	private List<Activity> activitys = new ArrayList<Activity>();

	public static ApplicationBean getApplication() {
		return Application;
	}

	@Override
	public void onCreate() {
		long time = System.currentTimeMillis();
//		registerActivityLifecycleCallbacks(callbacks);
		// 用来获取错误报告
		// -------------------------------------------------------------------------------------------------
		// ExceptionHandler handler = ExceptionHandler.getInstance(this);
		// Thread.setDefaultUncaughtExceptionHandler(handler);
		// -------------------------------------------------------------------------------------------------
		Application = this;
		logger = Logger.getLogger("debug");
		// 读取配置文件
		Properties properties = Handler_Properties.loadConfigAssets("mvc.properties");
		if (properties != null && properties.containsKey("standard_w")) {
			mode_w = Integer.valueOf(properties.get("standard_w").toString());
		}
		if (properties != null && properties.containsKey("standard_h")) {
			mode_h = Integer.valueOf(properties.get("standard_h").toString());
		}
		// --------------------------------------------------------------------------------------------------
		// 是否打开兼容模式
		boolean iscompatible = false;
		if (properties != null && properties.containsKey("iscompatible")) {
			iscompatible = Boolean.valueOf(properties.get("iscompatible").toString());
		}
		// --------------------------------------------------------------------------------------------------
		// 开启线程来提前遍历需要注入的activity
		initThread.start();
		// --------------------------------------------------------------------------------------------------
		// 整个框架的核心
		InjectViewUtils.setApplication(Application);
		// 反射获取mMainThread
		// getBaseContext()返回的是ContextImpl对象 ContextImpl中包含ActivityThread mMainThread这个对象
		Object mainThread = KernelObject.declaredGet(getBaseContext(), "mMainThread");
		// 反射获取mInstrumentation的对象
		Field instrumentationField = KernelReflect.declaredField(mainThread.getClass(), "mInstrumentation");
		instrumentation = new InstrumentationBean();
		// 自定义一个Instrumentation的子类 并把所有的值给copy进去
		if (iscompatible) {
			KernelObject.copy(KernelReflect.get(mainThread, instrumentationField), instrumentation);
		}
		// 再把替换过的Instrumentation重新放进去
		KernelReflect.set(mainThread, instrumentationField, instrumentation);
		// --------------------------------------------------------------------------------------------------
		super.onCreate();
		// --------------------------------------------------------------------------------------------------
		// 判断开启框架内的图片下载
		boolean isImageLoad = false;
		if (properties != null && properties.containsKey("imageload_open")) {
			isImageLoad = Boolean.valueOf(properties.get("imageload_open").toString());
		}
		// --------------------------------------------------------------------------------------------------
		// 开启框架内的图片下载控件
		if (isImageLoad) {
//			// 初始化图片下载控件 全局配置
//			GlobalConfig globalConfig = GlobalConfig.getInstance();
//			if (properties != null && properties.containsKey("memory_size")) {
//				globalConfig.setMemory_size(Integer.valueOf(properties.get("memory_size").toString()) * 1024 * 1024);
//			}
//			if (properties != null && properties.containsKey("maxWidth")) {
//				globalConfig.setMaxWidth(Integer.valueOf(properties.get("maxWidth").toString()));
//			}
//			if (properties != null && properties.containsKey("maxHeight")) {
//				globalConfig.setMaxHeight(Integer.valueOf(properties.get("maxHeight").toString()));
//			}
//			if (properties != null && properties.containsKey("def_drawable")) {
//				Integer id = InjectViewUtils.getResouceId("drawable", properties.get("def_drawable").toString());
//				if (id != null) {
//					globalConfig.setDef_drawable(getResources().getDrawable(id));
//				}
//			}
//			if (properties != null && properties.containsKey("failed_drawable")) {
//				Integer id = InjectViewUtils.getResouceId("drawable", properties.get("failed_drawable").toString());
//				if (id != null) {
//					globalConfig.setFailed_drawable(getResources().getDrawable(id));
//				}
//			}
//			// 本地图片加载线程池
//			if (properties != null && properties.containsKey("local_cpu")) {
//				globalConfig.setLocal_cpu(Integer.valueOf(properties.get("local_cpu").toString()));
//			}
//			// 网络图片加载线程池
//			if (properties != null && properties.containsKey("internet_cpu")) {
//				globalConfig.setInternet_cpu(Integer.valueOf(properties.get("internet_cpu").toString()));
//			}
//			globalConfig.init(this);
		}
		// --------------------------------------------------------------------------------------------------
		logger.d("appliaction 加载时间为:" + (System.currentTimeMillis() - time));
		init();
	}

	Thread initThread = new Thread() {
		public void run() {
			ContextUtils.getFactoryProvider();
		};
	};

	public int getMode_w() {
		return mode_w;
	}

	public void setMode_w(int mode_w) {
		this.mode_w = mode_w;
	}

	public int getMode_h() {
		return mode_h;
	}

	public void setMode_h(int mode_h) {
		this.mode_h = mode_h;
	}

	public void init() {

	};

	public void keypress(View view, final int key) {
		view.setFocusable(true);
		view.requestFocus();
		new Thread(new Runnable() {
			@Override
			public void run() {
				instrumentation.sendKeyDownUpSync(key);
			}
		}).start();
	}

	public DbUtils getDb() {
		return getDb(null, this.dbName);
	}

	public DbUtils getDb(String dbDirs, String dbName) {
		String key = dbDirs == null ? dbName : dbDirs + dbName;
		if (dbMap.containsKey(key)) {
			return dbMap.get(key);
		}

		DbUtils db;
		if (dbDirs == null) {
			db = DbUtils.create(this, dbName);
			dbMap.put(dbName, db);
		} else {
			File file = new File(dbDirs);
			if (!file.exists()) {
				file.mkdirs();
			}
			db = DbUtils.create(this, dbDirs, dbName);
			dbMap.put(dbDirs + dbName, db);
		}
		db.configDebug(true);
		db.configAllowTransaction(true);
		return db;
	}

	/**
	 * 避免由于InjectAll是静态的导致永远保留的是最后一次的
	 * @author gdpancheng@gmail.com 2014-5-4 下午2:21:48
	 * @return HashMap<String,Object>
	 */
	public List<Activity> getActivity() {
		return activitys;
	}

//	ActivityLifecycleCallbacks callbacks = new ActivityLifecycleCallbacks() {
//
//		@Override
//		public void onActivityStopped(Activity activity) {
//		}
//
//		@Override
//		public void onActivityStarted(Activity activity) {
//		}
//
//		@Override
//		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//		}
//
//		@Override
//		public void onActivityResumed(Activity activity) {
//		}
//
//		@Override
//		public void onActivityPaused(Activity activity) {
//		}
//
//		@Override
//		public void onActivityDestroyed(Activity activity) {
//			activitys.remove(activity);
//		}
//
//		@Override
//		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//		}
//	};
}
