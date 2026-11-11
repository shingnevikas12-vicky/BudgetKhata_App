package com.budgetkhata.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetkhata.R
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.BottomDestination
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.dashboard.DashboardScreen
import com.budgetkhata.ui.events.EventDetailScreen
import com.budgetkhata.ui.events.EventEditScreen
import com.budgetkhata.ui.events.EventsScreen
import com.budgetkhata.ui.expenses.ExpenseEditScreen
import com.budgetkhata.ui.expenses.ExpensesScreen
import com.budgetkhata.ui.payments.PaymentActionsHost
import com.budgetkhata.ui.reports.ReportsScreen
import com.budgetkhata.ui.settings.DataSettingsScreen
import com.budgetkhata.ui.settings.ReminderTemplateScreen
import com.budgetkhata.ui.settings.SettingsScreen
import com.budgetkhata.ui.settings.UpiSettingsScreen
import com.budgetkhata.ui.students.StudentDetailScreen
import com.budgetkhata.ui.students.StudentEditScreen
import com.budgetkhata.ui.students.StudentsScreen

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val baseRoute = currentRoute?.substringBefore('?')
    val showBottomBar = baseRoute in Routes.topLevelBases

    val destinations = listOf(
        BottomDestination(base = "home", route = Routes.HOME, label = stringResource(R.string.home), icon = Icons.Rounded.Home),
        BottomDestination(base = "students", route = Routes.students(), label = stringResource(R.string.students), icon = Icons.Rounded.PeopleAlt),
        BottomDestination(base = "events", route = Routes.EVENTS, label = stringResource(R.string.events), icon = Icons.Rounded.Event),
        BottomDestination(base = "expenses", route = Routes.expenses(), label = stringResource(R.string.expenses), icon = Icons.AutoMirrored.Rounded.ReceiptLong),
        BottomDestination(base = "settings", route = Routes.SETTINGS, label = stringResource(R.string.settings), icon = Icons.Rounded.Settings)
    )

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        PaymentActionsHost(container) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = baseRoute == destination.base,
                                    onClick = {
                                        if (baseRoute != destination.base) {
                                            navController.navigate(destination.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                                    label = { Text(destination.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    navGraph(navController, container)
                }
            }
        }
    }
}

private fun NavGraphBuilder.navGraph(navController: NavHostController, container: AppContainer) {
    composable(Routes.HOME) {
        DashboardScreen(
            container = container,
            onSearchStudents = { query -> navController.navigate(Routes.students(query = query)) },
            onFilterStudents = { status -> navController.navigate(Routes.students(status = status)) },
            onAddStudent = { navController.navigate(Routes.STUDENT_CREATE) },
            onAddEvent = { navController.navigate(Routes.EVENT_CREATE) },
            onOpenEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
            onOpenEvents = { navController.navigate(Routes.EVENTS) },
            onOpenReports = { navController.navigate(Routes.reports()) },
            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
        )
    }

    composable(
        route = Routes.STUDENTS,
        arguments = listOf(
            navArgument("query") { type = NavType.StringType; defaultValue = "" },
            navArgument("status") { type = NavType.StringType; defaultValue = "" }
        )
    ) {
        StudentsScreen(
            container = container,
            onOpenStudent = { studentId -> navController.navigate(Routes.studentDetail(studentId)) },
            onAddStudent = { navController.navigate(Routes.STUDENT_CREATE) },
            onEditStudent = { studentId -> navController.navigate(Routes.studentEdit(studentId)) },
            onOpenReports = { navController.navigate(Routes.reports()) }
        )
    }

    composable(Routes.STUDENT_CREATE) {
        StudentEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.STUDENT_DETAIL,
        arguments = listOf(navArgument("studentId") { type = NavType.LongType })
    ) {
        StudentDetailScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onEdit = { studentId -> navController.navigate(Routes.studentEdit(studentId)) },
            onOpenEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
            onDeleted = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.STUDENT_EDIT,
        arguments = listOf(navArgument("studentId") { type = NavType.LongType })
    ) {
        StudentEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(Routes.EVENTS) {
        EventsScreen(
            container = container,
            onOpenEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
            onAddEvent = { navController.navigate(Routes.EVENT_CREATE) },
            onEditEvent = { eventId -> navController.navigate(Routes.eventEdit(eventId)) },
            onOpenReports = { navController.navigate(Routes.reports()) }
        )
    }

    composable(Routes.EVENT_CREATE) {
        EventEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.EVENT_DETAIL,
        arguments = listOf(navArgument("eventId") { type = NavType.LongType })
    ) {
        EventDetailScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onEditEvent = { eventId -> navController.navigate(Routes.eventEdit(eventId)) },
            onOpenStudent = { studentId -> navController.navigate(Routes.studentDetail(studentId)) },
            onAddExpense = { eventId -> navController.navigate(Routes.expenseCreate(eventId)) },
            onEditExpense = { expenseId -> navController.navigate(Routes.expenseEdit(expenseId)) },
            onOpenReports = { eventId -> navController.navigate(Routes.reports(eventId)) }
        )
    }

    composable(
        route = Routes.EVENT_EDIT,
        arguments = listOf(navArgument("eventId") { type = NavType.LongType })
    ) {
        EventEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.EXPENSES,
        arguments = listOf(navArgument("eventId") { type = NavType.LongType; defaultValue = 0L })
    ) {
        ExpensesScreen(
            container = container,
            onAddExpense = { eventId -> navController.navigate(Routes.expenseCreate(eventId)) },
            onEditExpense = { expenseId -> navController.navigate(Routes.expenseEdit(expenseId)) },
            onOpenReports = { navController.navigate(Routes.reports()) }
        )
    }

    composable(
        route = Routes.EXPENSE_CREATE,
        arguments = listOf(navArgument("eventId") { type = NavType.LongType; defaultValue = 0L })
    ) {
        ExpenseEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.EXPENSE_EDIT,
        arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
    ) {
        ExpenseEditScreen(
            container = container,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = Routes.REPORTS,
        arguments = listOf(navArgument("eventId") { type = NavType.LongType; defaultValue = 0L })
    ) {
        ReportsScreen(
            container = container,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(
            container = container,
            onOpenUpi = { navController.navigate(Routes.SETTINGS_UPI) },
            onOpenReminder = { navController.navigate(Routes.SETTINGS_REMINDER) },
            onOpenData = { navController.navigate(Routes.SETTINGS_DATA) },
            onOpenReports = { navController.navigate(Routes.reports()) }
        )
    }

    composable(Routes.SETTINGS_UPI) {
        UpiSettingsScreen(container = container, onBack = { navController.popBackStack() })
    }

    composable(Routes.SETTINGS_REMINDER) {
        ReminderTemplateScreen(container = container, onBack = { navController.popBackStack() })
    }

    composable(Routes.SETTINGS_DATA) {
        DataSettingsScreen(container = container, onBack = { navController.popBackStack() })
    }
}
