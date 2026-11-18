# Pedidos360 · Backend

Repositorio del backend para EC2. Contiene los microservicios Spring Boot, PostgreSQL, RabbitMQ en clúster de tres nodos, Kafka de tres brokers con ZooKeeper, Kafka UI y Nginx como proxy de API. Todo se inicia con **un único** `docker-compose.yml`.

```text
Frontend Angular local :4200
  → HTTPS API Gateway (JWT Entra ID)
  → HTTP EC2 :80 / Nginx (solo /api/*)
  → microservicios Spring Boot
  → PostgreSQL, Kafka/ZooKeeper y RabbitMQ en la red privada de Docker
```

Este repositorio no contiene el frontend. La raíz de Nginx no sirve una página; API Gateway se utiliza para las rutas de API.

## Contenido

- `docker-compose.yml`: infraestructura y servicios de aplicación en un solo Compose.
- `Dockerfile`, `pom.xml`, `common/` y los cinco módulos Spring Boot: catálogo, pedidos, auditoría, reportes y administración de mensajería.
- `nginx/`: proxy HTTP que entrega las rutas `/api/...` a cada microservicio.
- `.env.example`: plantilla de configuración sin credenciales privadas.

## Preparar y levantar

En una máquina con Docker Compose v2 (Java y Maven solo son necesarios para ejecutar fuera de Docker):

```bash
cp .env.example .env
```

Edita `.env`: establece una contraseña de base de datos y confirma que el issuer/audience de Entra sean los de tu registro. Para RabbitMQ la demo usa `admin/admin`, como está configurado en el ejemplo. Si vas a reutilizar los volúmenes del despliegue anterior, conserva los valores anteriores de `DB_PASSWORD`, `RABBITMQ_ERLANG_COOKIE`, `RABBITMQ_USER` y `RABBITMQ_PASSWORD`; cambiar esas variables no cambia las credenciales ya almacenadas en los volúmenes.

```bash
docker compose up -d --build
docker compose ps
```

PostgreSQL y RabbitMQ/Kafka conservan los datos en volúmenes con nombre. `docker compose down` detiene y retira contenedores; no agregues `-v` si quieres conservar esos datos.

La pila completa consume memoria: una EC2 t3.micro tiene cerca de 1 GiB de RAM; los 8 GiB mencionados corresponden al disco. Con todos los brokers y microservicios ejecutándose, esa memoria puede agotarse. Si aparecen contenedores `OOMKilled` o reinicios, revisa `docker stats` y la memoria disponible antes de repetir el despliegue.

## API Gateway y frontend

En API Gateway configura una única integración HTTP al Nginx de esta instancia:

```text
http://<IPv4 pública actual de EC2>:80
```

Asigna `ANY /api/{proxy+}` a esa integración y aplica el JWT authorizer de Microsoft Entra:

- Identity source: `$request.header.Authorization`
- Issuer: `https://login.microsoftonline.com/1a1fed8a-8960-4a10-95a1-f9d77a98832a/v2.0`
- Audience: `d0b2e05c-58b9-4d02-aade-281d22bb3074`
- Authorization scope: `access_as_user`

Configura CORS en la HTTP API para el origen `http://localhost:4200`, métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS` y headers `Authorization, Content-Type, Accept`. En el Security Group permite TCP 80 para que Gateway alcance Nginx. La IP pública puede cambiar: al cambiar, actualiza el destino de esta misma integración; el frontend conserva su URL local y el Invoke URL de la API.

En Entra registra `http://localhost:4200/redirect` como Redirect URI de tipo SPA. El navegador abre el frontend en `http://localhost:4200`; no abras el Invoke URL como si fuera la página de Pedidos360.

## Acceso a herramientas de prueba

Los puertos publicados por Compose son:

- `80`: Nginx y rutas de API para API Gateway.
- `8080`: Kafka UI.
- `15672`: RabbitMQ Management; usuario/clave inicial del ejemplo: `admin/admin`.
- `5672`: protocolo AMQP de RabbitMQ.
- `9092`, `9093`, `9094`: listeners host de los tres brokers Kafka.
- `2181`: cliente ZooKeeper.
- `3000`: health check de Nginx, enlazado solo a `127.0.0.1`.

No se publica el puerto `5432`; PostgreSQL queda en la red Docker. Para acceder desde Internet a una herramienta de prueba, además de publicar el puerto, su Security Group debe permitirlo.

## Detener los Compose anteriores antes de migrar

El proyecto anterior ejecutaba la infraestructura y la aplicación desde dos Compose. Antes de iniciar este archivo en la misma EC2, detén los dos grupos viejos para liberar sus puertos. `stop` no borra los contenedores ni los volúmenes:

```bash
cd /home/ubuntu/Pedidos360/backend
docker compose stop
cd /home/ubuntu/Pedidos360
docker compose stop
```

Antes de detener la copia antigua, conserva su `backend/.env`. En la nueva carpeta de backend, úsalo si aún no existe `.env`; si nunca hubo uno, copia `.env.example` y configura sus valores. Después de copiar este repositorio a EC2, ejecuta `docker compose up -d --build` desde su raíz. Este Compose reutiliza los nombres de volúmenes actuales de PostgreSQL, Kafka y RabbitMQ para conservar los datos.

## Separación de repositorios

Esta carpeta es un repositorio independiente del frontend. Crea un repositorio GitHub vacío para el backend y, desde aquí, configura su remoto:

```bash
git remote add origin https://github.com/<usuario>/Pedidos360-Backend.git
git push -u origin main
```

No subas `.env`; está excluido por `.gitignore`.
"# Pedidos360-Backend" 
"# Pedidos360-Backend" 
