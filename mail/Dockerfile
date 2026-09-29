FROM debian:bookworm-slim
ENV DEBIAN_FRONTEND=noninteractive
RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d \
    && chmod +x /usr/sbin/policy-rc.d \
    && apt-get update \
    && apt-get install -y --no-install-recommends postfix ca-certificates python3 tini \
    && rm -rf /var/lib/apt/lists/*
COPY main.cf /etc/postfix/main.cf
COPY entrypoint.py /usr/local/bin/aincrad-mail
RUN chmod 755 /usr/local/bin/aincrad-mail \
    && awk 'NF>=8 && $1 !~ /^#/ && /^[^[:space:]]/ {$5="n"} {print}' /etc/postfix/master.cf > /etc/postfix/master.new \
    && mv /etc/postfix/master.new /etc/postfix/master.cf
# SMTP is private to the Compose network; there is no public port mapping.
EXPOSE 25
ENTRYPOINT ["/usr/bin/tini","--","/usr/local/bin/aincrad-mail"]
