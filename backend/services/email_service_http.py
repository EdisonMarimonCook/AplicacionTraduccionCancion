"""
Servicio de emails usando APIs HTTP (no SMTP)
Funciona en Render porque usa puerto 443 (HTTPS)
"""
import logging
import httpx
from pydantic import EmailStr
from backend.config import settings

logger = logging.getLogger(__name__)


async def send_email_via_brevo(to_email: str, subject: str, html_content: str):
    """Envía email usando Brevo API v3 (HTTP)"""
    url = "https://api.brevo.com/v3/smtp/email"
    headers = {
        "api-key": settings.BREVO_API_KEY,
        "Content-Type": "application/json"
    }
    payload = {
        "sender": {"email": settings.MAIL_USERNAME, "name": "Music Translator"},
        "to": [{"email": to_email}],
        "subject": subject,
        "htmlContent": html_content
    }
    
    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=payload, headers=headers, timeout=10.0)
        response.raise_for_status()
        logger.info(f"✅ Email enviado via Brevo a {to_email}")
        return response.json()


async def send_email_via_sendgrid(to_email: str, subject: str, html_content: str):
    """Envía email usando SendGrid API v3 (HTTP)"""
    url = "https://api.sendgrid.com/v3/mail/send"
    headers = {
        "Authorization": f"Bearer {settings.SENDGRID_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "personalizations": [{"to": [{"email": to_email}]}],
        "from": {"email": settings.MAIL_USERNAME, "name": "Music Translator"},
        "subject": subject,
        "content": [{"type": "text/html", "value": html_content}]
    }
    
    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=payload, headers=headers, timeout=10.0)
        response.raise_for_status()
        logger.info(f"✅ Email enviado via SendGrid a {to_email}")
        return response.json()


async def send_email(to_email: str, subject: str, html_content: str):
    """
    Envía email con fallback automático:
    1. Brevo API (300 emails/día gratis)
    2. SendGrid API (100 emails/día gratis)
    3. Falla si ninguno está configurado
    """
    # Intentar con Brevo primero
    if settings.BREVO_API_KEY:
        try:
            logger.info(f"📧 Enviando email via Brevo a {to_email}")
            return await send_email_via_brevo(to_email, subject, html_content)
        except Exception as e:
            logger.error(f"❌ Brevo falló: {e}")
            # Continuar con SendGrid
    
    # Fallback a SendGrid
    if settings.SENDGRID_API_KEY:
        try:
            logger.info(f"📧 Enviando email via SendGrid a {to_email}")
            return await send_email_via_sendgrid(to_email, subject, html_content)
        except Exception as e:
            logger.error(f"❌ SendGrid falló: {e}")
            raise
    
    # Si no hay API keys configuradas
    raise ValueError("❌ No hay servicio de email configurado (BREVO_API_KEY o SENDGRID_API_KEY)")


async def send_verification_code(email_to: EmailStr, code: str):
    """Envía el código de bienvenida para activar la cuenta"""
    logger.info(f"📧 Preparando email de verificación para {email_to}")
    
    html_content = f"""
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
        <h2 style="color: #FF5722;">🎵 Bienvenido a MusicTransIAtor</h2>
        <p>Gracias por registrarte. Para activar tu cuenta, introduce este código en la App:</p>
        <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 5px;">
            <h1 style="margin: 0; color: #333; letter-spacing: 5px;">{code}</h1>
        </div>
        <p style="font-size: 0.9em; color: #777;">Si no has sido tú, ignora este mensaje.</p>
    </div>
    """
    
    try:
        await send_email(
            to_email=email_to,
            subject="Verifica tu cuenta - MusicTransIAtor",
            html_content=html_content
        )
        logger.info(f"✅ Código de verificación enviado a {email_to}")
    except Exception as e:
        logger.error(f"❌ Error enviando verificación a {email_to}: {e}", exc_info=True)
        raise


async def send_password_reset_code(email_to: EmailStr, code: str):
    """Envía el código para recuperar contraseña"""
    logger.info(f"📧 Preparando email de recuperación para {email_to}")
    
    html_content = f"""
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
        <h2 style="color: #2196F3;">🔐 Recuperación de Contraseña</h2>
        <p>Hemos recibido una solicitud para restablecer tu contraseña. Usa este código:</p>
        <div style="background-color: #e3f2fd; padding: 15px; text-align: center; border-radius: 5px;">
            <h1 style="margin: 0; color: #0d47a1; letter-spacing: 5px;">{code}</h1>
        </div>
        <p>Este código expira en 10 minutos.</p>
        <p style="font-size: 0.9em; color: #777;">Si no has solicitado esto, ignora este mensaje.</p>
    </div>
    """
    
    try:
        await send_email(
            to_email=email_to,
            subject="Restablecer Contraseña - MusicTransIAtor",
            html_content=html_content
        )
        logger.info(f"✅ Código de recuperación enviado a {email_to}")
    except Exception as e:
        logger.error(f"❌ Error enviando recuperación a {email_to}: {e}", exc_info=True)
        raise
