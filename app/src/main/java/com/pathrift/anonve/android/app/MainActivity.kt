package com.pathrift.anonve.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pathrift.anonve.android.core.ui.PathriftNavGraph
import com.pathrift.anonve.android.core.ui.PathriftTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PathriftTheme {
                PathriftNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
