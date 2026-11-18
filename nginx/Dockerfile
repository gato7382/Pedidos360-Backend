FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN mkdir -p /etc/nginx/snippets
COPY api-routes.conf /etc/nginx/snippets/pedidos360-api-routes.conf
COPY proxy_params /etc/nginx/proxy_params
EXPOSE 80 3000
