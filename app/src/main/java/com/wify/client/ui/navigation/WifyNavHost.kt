package com.wify.client.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wify.client.ui.screens.DeviceDetailScreen
import com.wify.client.ui.screens.DeviceListScreen
import com.wify.client.ui.screens.MainScreen
import com.wify.client.ui.screens.RecordsScreen
import com.wify.client.ui.screens.ScanScreen
import com.wify.client.ui.viewmodel.DeviceDetailViewModel
import com.wify.client.ui.viewmodel.DeviceListViewModel
import com.wify.client.ui.viewmodel.MainViewModel
import com.wify.client.ui.viewmodel.RecordsViewModel
import com.wify.client.ui.viewmodel.ScanViewModel
import com.wify.client.ui.viewmodel.WifyViewModelFactory

/**
 * W.I.F.Y 흐름 기반 네비게이션.
 *
 * 홈 = 등록된 기기(DEVICE_LIST). 나머지는 모두 위에 push 되고 back은 popBackStack.
 *   등록된 기기 ─tap─▶ 기기 상세 ─선택─▶ 메인 모니터링
 *        │  └─추가─▶ 찾은 기기(스캔) ─연결─▶ (목록 복귀)
 *        ▼
 *   메인 ─기기─▶ 등록된 기기(복귀, 중복 없음) / ─기록─▶ 기록
 *
 * back 규칙: 하위 화면 = popBackStack, 홈 = 앱 종료(백스택 비면 finish).
 */
@Composable
fun WifyNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = WifyRoutes.DEVICE_LIST,
        modifier = modifier,
    ) {
        // 등록된 기기 (홈)
        composable(WifyRoutes.DEVICE_LIST) {
            val vm: DeviceListViewModel = viewModel(factory = WifyViewModelFactory())
            val state by vm.uiState.collectAsState()
            DeviceListScreen(
                state = state,
                onRegisterClick = { navController.navigate(WifyRoutes.SCAN) },
                onDeviceClick = { id -> navController.navigate(WifyRoutes.deviceDetail(id)) },
                onReset = vm::resetAll,
                onDeleteDevice = vm::deleteDevice,
                onBack = {
                    // 등록된 기기에서 back은 메인 모니터링으로
                    navController.navigate(WifyRoutes.MAIN) {
                        popUpTo(WifyRoutes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // 찾은 기기 (BLE 스캔)
        composable(WifyRoutes.SCAN) {
            val vm: ScanViewModel = viewModel(factory = WifyViewModelFactory())
            val state by vm.uiState.collectAsState()
            ScanScreen(
                state = state,
                onRescan = vm::rescan,
                onConnect = { device ->
                    vm.connect(device.deviceId, device.name) {
                        Toast.makeText(context, "‘${device.name}’ 기기가 연결되었어요", Toast.LENGTH_SHORT).show()
                        // 등록 후 목록으로 복귀
                        navController.popBackStack(WifyRoutes.DEVICE_LIST, inclusive = false)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // 기기 상세 (선택 + 인라인 이름수정 + 사진 변경)
        composable(
            route = WifyRoutes.DEVICE_DETAIL_PATTERN,
            arguments = listOf(navArgument(WifyRoutes.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(WifyRoutes.ARG_DEVICE_ID).orEmpty()
            val vm: DeviceDetailViewModel = viewModel(factory = WifyViewModelFactory(deviceId))
            val state by vm.uiState.collectAsState()
            DeviceDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onEditNameStart = vm::startEdit,
                onEditNameChange = vm::onNameChange,
                onSaveName = vm::saveName,
                onSelect = {
                    vm.select {
                        // 선택 후 메인으로. 상세는 비우고 [DEVICE_LIST, MAIN] 상태로.
                        navController.navigate(WifyRoutes.MAIN) {
                            popUpTo(WifyRoutes.DEVICE_LIST) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onChangeImage = vm::setImage,
            )
        }

        // 메인 모니터링
        composable(WifyRoutes.MAIN) {
            val vm: MainViewModel = viewModel(factory = WifyViewModelFactory())
            val state by vm.uiState.collectAsState()
            MainScreen(
                state = state,
                onDevicesClick = {
                    // 기기 목록으로 복귀 (중복 푸시 없이 기존 DEVICE_LIST로)
                    navController.navigate(WifyRoutes.DEVICE_LIST) {
                        popUpTo(WifyRoutes.DEVICE_LIST) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRecordsClick = { navController.navigate(WifyRoutes.RECORDS) },
                onRecalibrate = vm::recalibrate,
            )
        }

        // 기록 (로그 + 기기 필터)
        composable(WifyRoutes.RECORDS) {
            val vm: RecordsViewModel = viewModel(factory = WifyViewModelFactory())
            val state by vm.uiState.collectAsState()
            RecordsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelectDeviceFilter = vm::setFilter,
            )
        }
    }
}
