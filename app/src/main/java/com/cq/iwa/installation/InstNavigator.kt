package com.cq.iwa.installation

import android.content.Context
import android.content.Intent
import com.cq.iwa.feature.installation.network.InstActionConfigDto
import com.cq.iwa.feature.installation.network.InstJson
import com.cq.iwa.feature.installation.network.InstProjectDto

object InstNavigator {
    const val EXTRA_PROJECT = "project"
    const val EXTRA_TODO = "todo"
    const val EXTRA_PROJECT_ID = "projectId"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_SCHEMA = "jsonSchema"
    const val EXTRA_FORM = "lastFormData"
    const val EXTRA_TITLE = "title"
    const val EXTRA_ACTIONS = "actionConfigs"

    fun openPending(context: Context) {
        context.startActivity(Intent(context, InstPendingActivity::class.java))
    }

    fun openMy(context: Context) {
        context.startActivity(Intent(context, InstMyProjectsActivity::class.java))
    }

    fun openAll(context: Context) {
        context.startActivity(Intent(context, InstAllProjectsActivity::class.java))
    }

    fun openDetail(context: Context, project: InstProjectDto, browse: Boolean = false) {
        context.startActivity(
            Intent(context, InstDetailActivity::class.java)
                .putExtra(EXTRA_PROJECT, InstJson.encode(project))
                .putExtra(EXTRA_TODO, if (browse) 1 else 0),
        )
    }

    fun openVForm(
        context: Context,
        projectId: Int,
        taskId: String,
        schema: String,
        form: String?,
        title: String,
        actions: List<InstActionConfigDto>,
    ) {
        context.startActivity(
            Intent(context, InstVFormActivity::class.java)
                .putExtra(EXTRA_PROJECT_ID, projectId)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_SCHEMA, schema)
                .putExtra(EXTRA_FORM, form)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_ACTIONS, InstJson.encode(actions)),
        )
    }

    fun openMeters(context: Context, projectId: Int, editable: Boolean) {
        val cls = if (editable) InstMeterEditActivity::class.java else InstMeterRecordsActivity::class.java
        context.startActivity(Intent(context, cls).putExtra(EXTRA_PROJECT_ID, projectId))
    }

    fun openLog(context: Context, projectId: Int) {
        context.startActivity(Intent(context, InstLogActivity::class.java).putExtra(EXTRA_PROJECT_ID, projectId))
    }
}
