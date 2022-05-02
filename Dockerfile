FROM nginx

COPY ./target/public /usr/share/nginx/html
