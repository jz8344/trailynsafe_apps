# 📱 Sistema de Gestión de Perfil - App Móvil Android

## 🎯 Funcionalidades Implementadas

### 1️⃣ **Editar Perfil** (`EditProfileActivity`)
- ✅ **Disponible para TODOS** (cuentas normales y Google)
- Permite editar:
  - Nombre (min 2 caracteres, solo letras)
  - Apellidos (min 2 caracteres, solo letras)
  - Teléfono (10 dígitos exactos)

### 2️⃣ **Cambiar Email** (`ChangeEmailActivity`)
- ❌ **Solo para cuentas normales** (NO para Google)
- Validaciones:
  - Dominios permitidos: gmail.com, hotmail.com, outlook.com, etc.
  - No puede ser igual al actual
  - Requiere contraseña actual para confirmar
- Muestra dialog de confirmación de contraseña

### 3️⃣ **Cambiar Contraseña** (`ChangePasswordActivity`)
- ❌ **Solo para cuentas normales** (NO para Google)
- Flujo en 2 pasos:
  1. Validar contraseña actual (genera token temporal)
  2. Ingresar nueva contraseña (min 6 caracteres, debe coincidir)
- **Cierra todas las sesiones** y redirige a login

---

## 🔑 Lógica de Cuentas de Google

### **¿Cómo se detecta una cuenta de Google?**

**SessionManager:**
```kotlin
fun isGoogleAccount(): Boolean {
    return getAuthProvider() == "google" || getGoogleId() != null
}
```

- **Campo `auth_provider`**: "google" o null
- **Campo `google_id`**: ID único de Google o null

### **¿Qué pueden y qué NO pueden hacer?**

| Funcionalidad | Cuenta Normal | Cuenta Google |
|---------------|---------------|---------------|
| Editar nombre/apellidos/teléfono | ✅ SÍ | ✅ SÍ |
| Cambiar email | ✅ SÍ | ❌ NO (administrado por Google) |
| Cambiar contraseña | ✅ SÍ | ❌ NO (usa OAuth) |
| Cerrar sesión | ✅ SÍ | ✅ SÍ |

---

## 📂 Estructura de Archivos Creados

```
app/src/main/java/com/example/trailynapp/
├── ui/
│   └── profile/
│       ├── EditProfileActivity.kt          ← Editar perfil (TODOS)
│       ├── ChangeEmailActivity.kt          ← Cambiar email (solo normales)
│       └── ChangePasswordActivity.kt       ← Cambiar contraseña (solo normales)
├── utils/
│   └── SessionManager.kt                   ← Actualizado con auth_provider y google_id
└── fragments/
    └── ProfileFragment.kt                  ← Actualizado con botones condicionales
```

---

## 🔄 Flujo de Login con Google

```
Usuario → Botón Google
    ↓
GoogleAuthManager.signInWithGoogle()
    ↓
Obtener ID Token de Google
    ↓
POST /auth/google { id_token, device_name }
    ↓
Backend valida token con Google API
    ↓
Backend retorna:
  - usuario { google_id, auth_provider: "google" }
  - token (Laravel Sanctum)
    ↓
SessionManager.saveLoginSession(
    authProvider = "google",
    googleId = "123456789"
)
    ↓
MainActivity → ProfileFragment detecta isGoogleAccount() = true
    ↓
Oculta botones de "Cambiar Email" y "Cambiar Contraseña"
```

---

## 🎨 UI de ProfileFragment Actualizado

```
┌────────────────────────────────────┐
│  👤 Juan Pérez                     │
│  📧 juan@gmail.com                 │
│  📞 5512345678                     │
│  🔒 Cuenta de Google               │ ← Badge si es Google
├────────────────────────────────────┤
│  [✏️ Editar Perfil]                │ ← Siempre visible
│  [📧 Cambiar Correo]               │ ← Solo si NO es Google
│  [🔑 Cambiar Contraseña]           │ ← Solo si NO es Google
├────────────────────────────────────┤
│ ℹ️ Tu correo y contraseña se       │ ← Info si es Google
│   administran desde Google         │
├────────────────────────────────────┤
│  [🚪 Cerrar Sesión]                │ ← Siempre visible
└────────────────────────────────────┘
```

---

## 🔐 Seguridad Implementada

### **1. Tokens de Autenticación**
- Todas las requests usan header: `Authorization: Bearer {token}`
- Token generado por Laravel Sanctum

### **2. Validación de Contraseña Actual**
- Para cambiar email: Se solicita contraseña en dialog
- Para cambiar contraseña: Paso 1 valida contraseña actual
- Backend genera token temporal de validación (válido 5 min)

### **3. Cierre de Sesiones**
- Al cambiar contraseña: Backend revoca TODOS los tokens
- Usuario debe iniciar sesión nuevamente
- Previene acceso con contraseña antigua

### **4. Validaciones Frontend**
- **Nombres**: Solo letras y espacios
- **Teléfono**: Solo números, exactamente 10 dígitos
- **Email**: Formato válido + dominios permitidos
- **Contraseña**: Mínimo 6 caracteres

---

## 📡 Endpoints del Backend

### **Editar Perfil**
```http
POST /editar-perfil
Authorization: Bearer {token}
Content-Type: application/json

{
  "nombre": "Juan",
  "apellidos": "Pérez García",
  "telefono": "5512345678"
}
```

**Response 200:**
```json
{
  "message": "Perfil actualizado",
  "usuario": {
    "id": 1,
    "nombre": "Juan",
    "apellidos": "Pérez García",
    "correo": "juan@gmail.com",
    "telefono": "5512345678",
    "auth_provider": "google",
    "google_id": "123456789"
  }
}
```

### **Cambiar Email**
```http
POST /cambiar-correo
Authorization: Bearer {token}
Content-Type: application/json

{
  "nuevo_correo": "nuevo@gmail.com",
  "contrasena_actual": "password123"
}
```

**Response 200:**
```json
{
  "message": "Correo actualizado",
  "usuario": { ... }
}
```

**Error 401:**
```json
{
  "error": "Contraseña incorrecta"
}
```

**Error 422:**
```json
{
  "error": {
    "nuevo_correo": ["Este correo ya está registrado"]
  }
}
```

### **Validar Contraseña Actual**
```http
POST /validar-password-actual
Authorization: Bearer {token}
Content-Type: application/json

{
  "password_actual": "password123"
}
```

**Response 200:**
```json
{
  "message": "Contraseña validada",
  "token_validacion": "abc123xyz789"
}
```

### **Cambiar Contraseña**
```http
POST /cambiar-contrasena-autenticado
Authorization: Bearer {token}
Content-Type: application/json

{
  "nueva_contrasena": "newpass456",
  "nueva_contrasena_confirmation": "newpass456",
  "token_validacion": "abc123xyz789"
}
```

**Response 200:**
```json
{
  "message": "Contraseña actualizada. Por favor inicia sesión nuevamente."
}
```

---

## 🧪 Testing

### **Probar con Cuenta Normal**

1. Registrarse con correo/contraseña normal
2. Ir a Perfil
3. **Ver todos los botones:**
   - ✅ Editar Perfil
   - ✅ Cambiar Correo
   - ✅ Cambiar Contraseña
4. Probar editar nombre/teléfono
5. Probar cambiar email (requiere contraseña)
6. Probar cambiar contraseña (2 pasos)

### **Probar con Cuenta de Google**

1. Iniciar sesión con Google
2. Ir a Perfil
3. **Ver solo:**
   - ✅ Editar Perfil (nombre, apellidos, teléfono)
   - ❌ Cambiar Correo (oculto)
   - ❌ Cambiar Contraseña (oculto)
   - ℹ️ Mensaje: "Tu correo y contraseña se administran desde Google"
4. Probar editar nombre/teléfono (funciona)

---

## 🐛 Manejo de Errores

### **EditProfileActivity**
- Validación en tiempo real (onFocusChange)
- Mensajes de error en `TextInputLayout.error`
- Toast para errores de red
- ProgressBar durante request

### **ChangeEmailActivity**
- Validación de dominio permitido
- Dialog de contraseña con manejo de errores
- Distinción entre error 401 (contraseña) y 422 (email duplicado)

### **ChangePasswordActivity**
- 2 pasos con navegación back/forth
- Validación de coincidencia de contraseñas
- Mensaje claro al cerrar sesión
- Redireccionamiento automático a login

---

## 🎯 Próximos Pasos Recomendados

1. **Crear los layouts XML** para las 3 Activities:
   - `activity_edit_profile.xml`
   - `activity_change_email.xml`
   - `activity_change_password.xml`
   - `dialog_password_confirm.xml`

2. **Actualizar `fragment_profile.xml`**:
   - Agregar botones condicionales
   - Badge para cuenta de Google
   - Mensaje informativo

3. **Agregar las Activities al `AndroidManifest.xml`**:
   ```xml
   <activity android:name=".ui.profile.EditProfileActivity" />
   <activity android:name=".ui.profile.ChangeEmailActivity" />
   <activity android:name=".ui.profile.ChangePasswordActivity" />
   ```

4. **Testing exhaustivo** con ambos tipos de cuenta

---

**Fecha de implementación:** 19 de noviembre de 2025  
**Versión del sistema:** 1.0.0
