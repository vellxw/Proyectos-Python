# Dictado por comas

Aplicación Android nativa para estudiar o escribir al dictado sin mirar el celular.

## Qué hace

- Pega textos largos directamente desde el portapapeles.
- También aparece en el menú **Compartir** de otras aplicaciones que envían texto.
- Divide automáticamente el texto al encontrar comas, punto y coma, dos puntos, saltos de línea y finales de oración.
- Repite cada fragmento entre 1 y 5 veces **en total**.
- Permite cambiar la velocidad de 0,5x a 2,0x.
- Permite regular la pausa entre repeticiones y la pausa antes del fragmento siguiente.
- Tiene controles de anterior, siguiente, pausar, reanudar y detener.
- Usa un servicio de reproducción en primer plano y un bloqueo parcial de CPU para seguir hablando con la pantalla apagada.
- Incluye controles desde la notificación.
- No necesita conexión a Internet: utiliza el motor de texto a voz instalado en Android.

## Compilar

El workflow de GitHub Actions incluido genera un APK de depuración instalable en:

`app/build/outputs/apk/debug/app-debug.apk`

## Uso recomendado para dictado

- Velocidad: 0,7x u 0,8x.
- Repeticiones: 2 veces totales.
- Pausa antes de repetir: 0,8 segundos.
- Pausa antes del fragmento siguiente: 2,5 a 4 segundos.

En celulares con administración agresiva de batería, abrir **Ajustes de esta app** y elegir uso de batería **Sin restricciones**.
