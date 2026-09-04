package com.cq.iwa.diagnose

import android.content.Context
import android.content.Intent

object DiagnoseNavigator {

    fun open(context: Context) {
        context.startActivity(Intent(context, DiagnoseTypeListActivity::class.java))
    }

    fun openCommonNb(context: Context) {
        context.startActivity(Intent(context, CommonNbDiagnoseActivity::class.java))
    }

    fun openDalianNb(context: Context) {
        context.startActivity(Intent(context, DalianNbDiagnoseActivity::class.java))
    }

    fun openWired(context: Context) {
        context.startActivity(Intent(context, WiredDiagnoseActivity::class.java))
    }
}
