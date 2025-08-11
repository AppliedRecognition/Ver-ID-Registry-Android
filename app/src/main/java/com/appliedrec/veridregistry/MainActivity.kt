package com.appliedrec.veridregistry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appliedrec.veridregistry.ui.theme.VerIDRegistryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerIDRegistryTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeView(navController)
                    }
                    composable(
                        route = "user/{userName}?editable={editable}",
                        arguments = listOf(
                            navArgument("userName") { type = NavType.StringType },
                            navArgument("editable") {
                                type = NavType.BoolType
                                defaultValue = true
                            }
                        )
                    ) { backStackEntry ->
                        backStackEntry.arguments?.getString("userName")?.let { userName ->
                            val editable = backStackEntry.arguments?.getBoolean("editable") ?: true
                            UserView(userName, editable)
                        }
                    }
                    composable("register") {
                        RegistrationIntroView(navController)
                    }
                    composable("registration_review") {
                        RegistrationReviewView(navController)
                    }
                    composable("settings") {
                        SettingsView(navController)
                    }
                    composable("users") {
                        UsersView(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VerIDRegistryTheme {
        Greeting("Android")
    }
}