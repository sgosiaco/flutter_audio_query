//The MIT License
//
//        Copyright (C) <2019>  <Marcos Antonio Boaventura Feitoza> <scavenger.gnu@gmail.com>
//
//        Permission is hereby granted, free of charge, to any person obtaining a copy
//        of this software and associated documentation files (the "Software"), to deal
//        in the Software without restriction, including without limitation the rights
//        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//        copies of the Software, and to permit persons to whom the Software is
//        furnished to do so, subject to the following conditions:
//
//        The above copyright notice and this permission notice shall be included in
//        all copies or substantial portions of the Software.
//
//        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//        THE SOFTWARE.
package boaventura.com.devel.br.flutteraudioquery

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import boaventura.com.devel.br.flutteraudioquery.delegate.AudioQueryDelegate
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar

/** FlutterAudioQueryPlugin  */
class FlutterAudioQueryPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {
    private var m_delegate: AudioQueryDelegate? = null
    private var m_pluginBinding: FlutterPluginBinding? = null
    private var m_activityBinding: ActivityPluginBinding? = null
    private var channel: MethodChannel? = null
    private var application: Application? = null

    // These are null when not using v2 embedding;
    private var lifecycle: Lifecycle? = null
    private var observer: LifeCycleObserver? = null

    private constructor(delegate: AudioQueryDelegate?) {
        m_delegate = delegate
    }

    constructor() {}

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val source = call!!.argument<String?>("source")
        if (source != null) {
            when (source) {
                "artist" -> m_delegate!!.artistSourceHandler(call, result!!)
                "album" -> m_delegate!!.albumSourceHandler(call, result!!)
                "song" -> m_delegate!!.songSourceHandler(call, result!!)
                "genre" -> m_delegate!!.genreSourceHandler(call, result!!)
                "playlist" -> m_delegate!!.playlistSourceHandler(call, result!!)
                "artwork" -> m_delegate!!.artworkSourceHandler(call, result!!)
                else -> result!!.error("unknown_source",
                        "method call was made by an unknown source", null)
            }
        } else {
            result!!.error("no_source", "There is no source in your method call", null)
        }
    }

    // embeding V2 implementation
    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        m_pluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        tearDown()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        m_activityBinding = binding
        Log.i("AUDIO_QUERY", "Using V2 EMBEDDING:: activity = " + binding!!.activity)
        setup(
                m_pluginBinding!!.binaryMessenger,
                m_pluginBinding!!.applicationContext as Application,
                m_activityBinding!!.activity,
                null,
                m_activityBinding
        )
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {}
    private fun setup(
            messenger: BinaryMessenger?,
            application: Application?,
            activity: Activity?, registrar: Registrar?,
            activityBinding: ActivityPluginBinding?) {
        this.application = application
        if (registrar != null) {
            // V1 embedding  delegate creation
            m_delegate = AudioQueryDelegate.instance(registrar)
            observer = LifeCycleObserver(activity)
            application!!.registerActivityLifecycleCallbacks(observer)
        } else {
            // V2 embedding setup for activity listeners.
            if (m_delegate == null) m_delegate = AudioQueryDelegate.instance(application!!.applicationContext, activity!!)
            activityBinding!!.addRequestPermissionsResultListener(m_delegate!!)
            lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding)

            //activityBinding.
            observer = LifeCycleObserver(activityBinding.activity)
            lifecycle!!.addObserver(observer!!)
        }
        if (channel == null) {
            channel = MethodChannel(messenger, CHANNEL_NAME)
            channel!!.setMethodCallHandler(FlutterAudioQueryPlugin(m_delegate))
        }
    }

    private fun tearDown() {
        if (m_activityBinding != null) {
            m_activityBinding!!.removeRequestPermissionsResultListener(m_delegate!!)
            m_activityBinding = null
        }
        if (lifecycle != null) {
            lifecycle!!.removeObserver(observer!!)
            lifecycle = null
        }
        m_delegate = null
        if (channel != null) {
            channel!!.setMethodCallHandler(null)
            channel = null
        }
        if (application != null) {
            application!!.unregisterActivityLifecycleCallbacks(observer)
            application = null
        }
    }

    private inner class LifeCycleObserver internal constructor(private val thisActivity: Activity?) : ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {}
        override fun onStart(owner: LifecycleOwner) {}
        override fun onResume(owner: LifecycleOwner) {}
        override fun onPause(owner: LifecycleOwner) {}
        override fun onStop(owner: LifecycleOwner) {
            onActivityStopped(thisActivity)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onActivityDestroyed(thisActivity)
        }

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity?) {}
        override fun onActivityResumed(activity: Activity?) {}
        override fun onActivityPaused(activity: Activity?) {}
        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
        override fun onActivityDestroyed(activity: Activity?) {
            if (thisActivity === activity && activity!!.applicationContext != null) {
                (activity.applicationContext as Application)
                        .unregisterActivityLifecycleCallbacks(
                                this) // Use getApplicationContext() to avoid casting failures
            }
        }

        override fun onActivityStopped(activity: Activity?) {}
    } //// LifeCycleObserver end

    companion object {
        private val CHANNEL_NAME: String? = "boaventura.com.devel.br.flutteraudioquery"
        fun registerWith(registrar: Registrar?) {
            if (registrar!!.activity() == null) return
            var application: Application? = null
            if (registrar.context() != null) {
                application = registrar.context().applicationContext as Application
            }
            val plugin = FlutterAudioQueryPlugin()
            Log.i("AUDIO_QUERY", "Using V1 EMBEDDING")
            plugin.setup(registrar.messenger(), application, registrar.activity(), registrar, null)
        }
    }
} // end FlutterAudioQueryPlugin
