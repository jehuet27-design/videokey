package com.example.videokey

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.videokey.ui.main.VideoKeyScreen
import com.example.videokey.ui.main.VideoKeyViewModel

@Composable
fun MainNavigation(viewModel: VideoKeyViewModel = viewModel()) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          VideoKeyScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
      },
  )
}
