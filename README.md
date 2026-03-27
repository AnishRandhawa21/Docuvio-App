# 📄 Docuvio

**Docuvio** is a modern document management app designed to help users organize, explore, and interact with documents in a simple and efficient way.

> Built for speed, simplicity, and a seamless user experience.

---

## ✨ Features

* 📁 Create and manage documents easily
* 🔍 Smart search and filtering
* ❤️ Like / save your favorite documents
* 🧭 Clean and intuitive navigation
* 🎯 Minimal and modern UI
* ⚡ Smooth and fast performance

---

## 📱 App Screens

![Login](https://github.com/user-attachments/assets/e9afd561-7c7e-473a-a315-f16e42376fa5)
![home](https://github.com/user-attachments/assets/0c1e323f-23e0-49e3-86c3-12e9398e9e3d)
![Order](https://github.com/user-attachments/assets/b3f8f404-8abd-4533-a79e-abf5e68d1206)
![order 2](https://github.com/user-attachments/assets/434b9926-92f6-4cdd-a062-3811cf39f3c8)
![OrderScreen](https://github.com/user-attachments/assets/0d50f098-1767-4db8-8a77-bb23843e2add)
![orderscreen2](https://github.com/user-attachments/assets/35d048f2-6e42-4649-85b0-176fc7b5b2b3)



---

## 🎯 Use Case

Docuvio helps users:

* Organize their personal or professional documents
* Quickly find important content
* Keep track of useful or favorite documents
* Maintain a clean and structured digital workspace

---

## 🛠️ Built With

* Kotlin (Android)
* Jetpack Compose
* Material 3 Design

---

## 📂 Project Structure

```
app/
├── core/
│   ├── auth/
│   │   └── TokenManager.kt
│   ├── network/
│       ├── ApiClient.kt
│       └── OffsetDateTimeAdapter.kt
├── data/
│   ├── api/
│   │   ├── AuthApi.kt
│   │   ├── NotificationApi.kt
│   │   ├── OrderApi.kt
│   │   └── ShopApi.kt
│   ├── local/
│   ├── model/
│   │   ├── AttachDocumentRequest.kt
│   │   ├── Auth.kt
│   │   ├── CreatePaymentRequest.kt
│   │   ├── CreatePaymentResponse.kt
│   │   ├── Order.kt
│   │   ├── PrintOptions.kt
│   │   ├── RazorpayHolder.kt
│   │   ├── Shop.kt
│   │   ├── ShopListResponse.kt
│   │   ├── VerifyPaymentRequest.kt
│   │   ├── VerifyPaymentResponse.kt
│   │   └── WalkInOrderRequest.kt
│   ├── repository/
│       ├── AuthRepository.kt
│       ├── OrderRepository.kt
│       └── ShopRepository.kt
├── di/
│   └── AppContainer.kt
├── firebase/
│   ├── FcmService.kt
│   ├── FcmTokenManager.kt
│   ├── Fcmregistrar.kt
│   └── NotificationHelper.kt
├── theme/
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── SignupScreen.kt
│   ├── home/
│   │   └── HomeScreen.kt
│   ├── main/
│   │   └── MainScreen.kt
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Routes.kt
│   ├── order/
│   │   ├── utils/
│   │   │   ├── DateUtils.kt
│   │   │   ├── Floatingpaybar.kt
│   │   │   ├── PricingUtils.kt
│   │   │   ├── TimeUtils.kt
│   │   │   └── WalkInFloatingpayBar.kt
│   │   ├── CreateOrderScreen.kt
│   │   ├── FilePreview.kt
│   │   └── WalkInOrderScreen.kt
│   ├── orders/
│   │   └── OrdersScreen.kt
│   ├── profile/
│   │   ├── DeleteAccountScreen.kt
│   │   ├── FeedbackScreen.kt
│   │   └── ProfileScreen.kt
│   ├── splash/
│   │   ├── SplashScreen.kt
│   │   └── SplashViewModel.kt
│   ├── terms/
│       └── TermsScreen.kt
├── utils/
│   ├── AppPreferences.kt
│   ├── DateUtils.kt
│   ├── FileUtils.kt
│   ├── OpenMail,kt.kt
│   ├── PasswordValidator.kt
│   ├── PdfUtils.kt
│   ├── ShopStatusResolver.kt
│   └── ShopTimeUtils.kt
├── viewmodel/
│   ├── AuthViewModel.kt
│   ├── AuthViewModelFactory.kt
│   ├── CreateOrderViewModel.kt
│   ├── CreateOrderViewModelFactory.kt
│   ├── HomeViewModel.kt
│   ├── HomeViewModelFactory.kt
│   ├── OrdersViewModel.kt
│   ├── OrdersViewModelFactory.kt
│   ├── ProfileViewModel.kt
│   ├── ProfileViewModelFactory.kt
│   ├── SplashViewModelFactory.kt
│   ├── WalkInOrderViewModel.kt
│   └── WalkInOrderViewModelFactory.kt
├── DocuvioApp.kt
└── MainActivity.kt

```

---

## ⚙️ Getting Started

1. Clone the repository:

```bash
git clone https://github.com/AnishRandhawa21/docuvio-App.git
```

2. Open in Android Studio

3. Sync Gradle

4. Run on emulator or device

---

## 🚀 Future Plans

* 🤝 Collaboration features
* 🧠 Smart suggestions
* 📤 Export documents
* 🌐 Cross-platform support

---

## 🤝 Contributing

Feel free to contribute!

1. Fork the repo
2. Create a new branch
3. Make changes
4. Submit a pull request

---

## 📬 Contact

* GitHub: https://github.com/AnishRandhawa21
  
---

## ⭐ Support

If you like this project, give it a ⭐ on GitHub!

---
