package com.bumptech.glide.module

/**
 * An interface allowing lazy configuration of Glide including setting options using [ ] and registering [ ModelLoaders][com.bumptech.glide.load.model.ModelLoader].
 *
 *
 * To use this interface:
 *
 *
 *  1. Implement the GlideModule interface in a class with public visibility, calling [       ][Registry.prepend] for each [       ] you'd like to register:
 * <pre>
 * `
 * public class FlickrGlideModule implements GlideModule {
 * @Override
 * public void applyOptions(Context context, GlideBuilder builder) {
 * builder.setDecodeFormat(DecodeFormat.ALWAYS_ARGB_8888);
 * }
 *
 * @Override
 * public void registerComponents(Context context, Glide glide) {
 * glide.register(Model.class, Data.class, new MyModelLoader());
 * }
 * }
` *
</pre> *
 *  1. Add your implementation to your list of keeps in your proguard.cfg file:
 * <pre>`-keepnames class * com.bumptech.glide.samples.flickr.FlickrGlideModule
`</pre> *
 *  1. Add a metadata tag to your AndroidManifest.xml with your GlideModule implementation's fully
 * qualified classname as the key, and `GlideModule` as the value:
 * <pre>`<meta-data
 * android:name="com.bumptech.glide.samples.flickr.FlickrGlideModule"
 * android:value="GlideModule" />
`</pre> *
 *
 *
 *
 * All implementations must be publicly visible and contain only an empty constructor so they can
 * be instantiated via reflection when Glide is lazily initialized.
 *
 *
 * There is no defined order in which modules are called, so projects should be careful to avoid
 * applying conflicting settings in different modules. If an application depends on libraries that
 * have conflicting modules, the application should consider avoiding the library modules and
 * instead providing their required dependencies in a single application module.
 *
 */
@Deprecated("""Libraries should use {@link LibraryGlideModule} and Applications should use {@link
 *     AppGlideModule}.""")
interface GlideModule : RegistersComponents, AppliesOptions