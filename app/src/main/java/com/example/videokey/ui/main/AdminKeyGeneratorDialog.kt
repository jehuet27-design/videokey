package com.example.videokey.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.videokey.data.VipLicenseManager

// Cryptographically secure hash of secret PIN (never stored in plaintext)
private const val ADMIN_PIN_HASH = "A5F90B3B894A1F466D956B5D26E79007BC830810E6B85E41F53115F55D056B3C"

private fun verifyAdminPin(entered: String): Boolean {
    return try {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(entered.trim().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02X".format(it) }
        hex.equals(ADMIN_PIN_HASH, ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminKeyGeneratorDialog(
    theme: AppTheme = AppTheme.CYBER_NEON,
    isTagalog: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var isUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    var deviceIdInput by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf("") }

    val bgColor = Color(0xFF0B1120)
    val cardBg = Color(0xFF1E293B)
    val cardBorder = Color(0xFF334155)
    val primaryGold = Color(0xFFFACC15)
    val cyanAccent = Color(0xFF38BDF8)
    val greenAccent = Color(0xFF10B981)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isUnlocked) primaryGold else cardBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isUnlocked) {
                    // ==========================================
                    // 1. PIN AUTHENTICATION SCREEN
                    // ==========================================
                    Text(text = "🔒", fontSize = 36.sp)

                    Text(
                        text = "Admin Access Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryGold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isTagalog)
                            "Ilagay ang iyong Secret PIN upang buksan ang VIP Key Generator."
                        else
                            "Enter your secret PIN to access the VIP Key Generator.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            isPinError = false
                        },
                        placeholder = {
                            Text(
                                text = "Ilagay ang Admin PIN...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (verifyAdminPin(pinInput)) {
                                    isUnlocked = true
                                    isPinError = false
                                } else {
                                    isPinError = true
                                }
                            }
                        ),
                        trailingIcon = {
                            Text(
                                text = if (showPassword) "👁️" else "🙈",
                                modifier = Modifier
                                    .clickable { showPassword = !showPassword }
                                    .padding(8.dp),
                                fontSize = 16.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgColor,
                            unfocusedContainerColor = bgColor,
                            focusedBorderColor = primaryGold,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isPinError) {
                        Text(
                            text = if (isTagalog) "❌ Maling Admin PIN / Password!" else "❌ Incorrect Admin PIN / Password!",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (verifyAdminPin(pinInput)) {
                                isUnlocked = true
                                isPinError = false
                            } else {
                                isPinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "🔓 I-UNLOCK ANG GENERATOR" else "🔓 UNLOCK GENERATOR",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "Isara" else "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // ==========================================
                    // 2. UNLOCKED ADMIN KEY GENERATOR SCREEN
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "👑", fontSize = 26.sp)
                            Column {
                                Text(
                                    text = "VIP Key Generator",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryGold
                                )
                                Text(
                                    text = "Jerome Huet Admin Console",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = greenAccent.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, greenAccent)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = greenAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorder, thickness = 1.dp)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "Device ID ng Customer:" else "Customer Device ID:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = deviceIdInput,
                                onValueChange = { deviceIdInput = it },
                                placeholder = {
                                    Text(
                                        text = "hal. VK-5FC0-0E77",
                                        color = Color.DarkGray,
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = bgColor,
                                    unfocusedContainerColor = bgColor,
                                    focusedBorderColor = primaryGold,
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = TextStyle(
                                    fontSize = 13.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (clip.isNotBlank()) {
                                        deviceIdInput = clip
                                        Toast.makeText(context, if (isTagalog) "Na-paste ang Device ID" else "Device ID pasted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                Text(text = "📋 PASTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cleanInput = deviceIdInput.trim()
                            if (cleanInput.isBlank()) {
                                Toast.makeText(context, if (isTagalog) "Paki-lagay ang Device ID!" else "Please enter Device ID!", Toast.LENGTH_SHORT).show()
                            } else {
                                val key = VipLicenseManager.generateLicenseKey(cleanInput)
                                generatedKey = key
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(
                            text = if (isTagalog) "⚡ I-GENERATE ANG VIP KEY" else "⚡ GENERATE VIP KEY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }

                    if (generatedKey.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.5.dp, primaryGold, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isTagalog) "IPADALA ANG KEY NA ITO SA BUYER:" else "SEND THIS KEY TO BUYER:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )

                            Text(
                                text = generatedKey,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = cyanAccent,
                                letterSpacing = 2.sp
                            )

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(generatedKey))
                                    Toast.makeText(
                                        context,
                                        if (isTagalog) "✅ Nakopya ang VIP Key: $generatedKey" else "✅ VIP Key copied: $generatedKey",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isTagalog) "📋 Kopyahin ang Key" else "📋 Copy Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Admin Quick Reference Info Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor.copy(alpha = 0.6f))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💡 Universal Master Key (Pang-lahat): VIP8888",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                        Text(
                            text = "📱 GCash / Maya: 09203732202 (Jerome Huet) • ₱99",
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(text = if (isTagalog) "Isara" else "Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
