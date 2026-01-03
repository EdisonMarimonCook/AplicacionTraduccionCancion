import smtplib
import logging
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from config import settings, get_email_password
from fastapi_mail import FastMail, MessageSchema, ConnectionConfig, MessageType
from pydantic import EmailStr
from config import settings

logger = logging.getLogger(__name__)

# 🚀 Configuración automática: Brevo > SendGrid > Gmail
if settings.BREVO_API_KEY:
    # Brevo/Sendinblue (300 emails/día gratis - MEJOR OPCIÓN)
    logger.info("📧 Usando Brevo/Sendinblue para emails")
    conf = ConnectionConfig(
        MAIL_USERNAME=settings.MAIL_USERNAME,  # Tu email verificado en Brevo
        MAIL_PASSWORD=settings.BREVO_API_KEY,   # API Key de Brevo
        MAIL_FROM=settings.MAIL_USERNAME,
        MAIL_PORT=587,
        MAIL_SERVER="smtp-relay.brevo.com",
        MAIL_STARTTLS=True,
        MAIL_SSL_TLS=False,
        USE_CREDENTIALS=True,
        VALIDATE_CERTS=True
    )
elif settings.SENDGRID_API_KEY:
    # SendGrid (usa API, no SMTP - funciona en Render)
    logger.info("📧 Usando SendGrid para emails")
    conf = ConnectionConfig(
        MAIL_USERNAME="apikey",  # SendGrid usa literal "apikey"
        MAIL_PASSWORD=settings.SENDGRID_API_KEY,
        MAIL_FROM=settings.MAIL_USERNAME,
        MAIL_PORT=587,
        MAIL_SERVER="smtp.sendgrid.net",
        MAIL_STARTTLS=True,
        MAIL_SSL_TLS=False,
        USE_CREDENTIALS=True,
        VALIDATE_CERTS=True
    )
else:
    # Gmail (solo funciona en desarrollo local)
    logger.info("📧 Usando Gmail para emails")
    conf = ConnectionConfig(
        MAIL_USERNAME=settings.MAIL_USERNAME,
        MAIL_PASSWORD=settings.MAIL_PASSWORD,
        MAIL_FROM=settings.MAIL_USERNAME,
        MAIL_PORT=465,
        MAIL_SERVER=settings.MAIL_SERVER,
        MAIL_STARTTLS=False,
        MAIL_SSL_TLS=True,
        USE_CREDENTIALS=True,
        VALIDATE_CERTS=True
    )

async def send_verification_code(email_to: EmailStr, code: str):
    """Envía el código de bienvenida para activar la cuenta"""
    logger.info(f"📧 Preparando email para {email_to} con código {code}")
    
    html = f"""
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
        <h2 style="color: #FF5722;">🎵 Bienvenido a MusicTransIAtor</h2>
        <p>Gracias por registrarte. Para activar tu cuenta, introduce este código en la App:</p>
        <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 5px;">
            <h1 style="margin: 0; color: #333; letter-spacing: 5px;">{code}</h1>
        </div>
        <p style="font-size: 0.9em; color: #777;">Si no has sido tú, ignora este mensaje.</p>
    </div>
    """

    message = MessageSchema(
        subject="Verifica tu cuenta - MusicTransIAtor",
        recipients=[email_to],
        body=html,
        subtype=MessageType.html
    )

    fm = FastMail(conf)
    try:
        logger.info(f"🚀 Enviando email a {email_to}...")
        await fm.send_message(message)
        logger.info(f"✅ Correo de verificación enviado exitosamente a {email_to}")
    except Exception as e:
        logger.error(f"❌ Error enviando correo a {email_to}: {e}", exc_info=True)

async def send_password_reset_code(email_to: EmailStr, code: str):
    """Envía el código para recuperar contraseña"""
    html = f"""
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
        <h2 style="color: #2196F3;">🔐 Recuperación de Contraseña</h2>
        <p>Hemos recibido una solicitud para restablecer tu contraseña. Usa este código:</p>
        <div style="background-color: #e3f2fd; padding: 15px; text-align: center; border-radius: 5px;">
            <h1 style="margin: 0; color: #0d47a1; letter-spacing: 5px;">{code}</h1>
        </div>
        <p>Este código expira en 10 minutos.</p>
    </div>
    """

    message = MessageSchema(
        subject="Restablecer Contraseña - MusicTransIAtor",
        recipients=[email_to],
        body=html,
        subtype=MessageType.html
    )

    fm = FastMail(conf)
    try:
        await fm.send_message(message)
        logger.info(f"📧 Correo de recuperación enviado a {email_to}")
    except Exception as e:
        logger.error(f"❌ Error enviando correo recuperación: {e}")

def send_verification_email(to_email: str, verification_code: str):
    """Envía el código de verificación para activar la cuenta"""
    html = f"""
    <html>
        <body>
            <h2>Bienvenido a MusicTransIAtor</h2>
            <p>Gracias por registrarte. Para activar tu cuenta, usa este código:</p>
            <h1 style="letter-spacing: 5px;">{verification_code}</h1>
            <p>Si no has sido tú quien se registró, ignora este correo.</p>
        </body>
    </html>
    """

    msg = MIMEMultipart()
    msg['From'] = settings.MAIL_USERNAME
    msg['To'] = to_email
    msg['Subject'] = 'Verifica tu cuenta - MusicTransIAtor'

    # Cuerpo del mensaje
    msg.attach(MIMEText(html, 'html'))

    try:
        # Conexión al servidor SMTP
        with smtplib.SMTP(settings.MAIL_SERVER, settings.MAIL_PORT) as server:
            server.starttls()  # Iniciar TLS
            smtp_password = get_email_password()  # 🔥 Usar helper con fallback
            server.login(settings.MAIL_USERNAME, smtp_password)
            server.send_message(msg)
        logger.info(f"📧 Correo de verificación enviado a {to_email}")
    except Exception as e:
        logger.error(f"❌ Error enviando correo de verificación: {e}")