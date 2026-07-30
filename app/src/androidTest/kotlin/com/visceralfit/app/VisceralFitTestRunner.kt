package com.visceralfit.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps [VisceralFitApplication] for Hilt's test application so instrumentation
 * tests can replace bindings. Referenced from `app/build.gradle.kts` as the
 * `testInstrumentationRunner`; renaming one without the other breaks every
 * instrumentation test with an unhelpful ClassNotFoundException.
 */
class VisceralFitTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
