"""Cliente publicador AMQP 1.0 (ActiveMQ Classic).

Publica N mensagens (padrão 5) com intervalo configurável (padrão 60 s) em
queue://<nome> (ponto a ponto) ou topic://<nome> (fan-out / pub-sub).
--persistent marca a mensagem como durable (header AMQP durable=true),
que o ActiveMQ grava em disco (KahaDB).
"""
import argparse, sys
from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container
from common import BROKER, log, conectar


class Publisher(MessagingHandler):
    def __init__(self, address, total, interval, persistent):
        super().__init__()
        self.address, self.total = address, total
        self.interval, self.persistent = interval, persistent
        self.sent = self.confirmed = 0
        self.sender = None

    def on_start(self, event):
        conn = conectar(event.container)
        self.sender = event.container.create_sender(conn, self.address)
        log("PUB", f"conectado em {BROKER}; destino={self.address} "
                   f"persistente={self.persistent} intervalo={self.interval}s")

    def on_sendable(self, event):
        if self.sent == 0:
            self._send()

    def _send(self):
        self.sent += 1
        body = f"mensagem-{self.sent}"
        msg = Message(body=body, durable=self.persistent, id=self.sent)
        self.sender.send(msg)
        log("PUB", f"enviada '{body}' ({self.sent}/{self.total})")

    def on_accepted(self, event):
        self.confirmed += 1
        log("PUB", f"broker confirmou (ACCEPTED) {self.confirmed}/{self.total}")
        if self.confirmed == self.total:
            event.connection.close()
        elif self.sent < self.total:
            event.container.schedule(self.interval, self)

    def on_timer_task(self, event):
        self._send()

    def on_connection_error(self, event):
        log("PUB", f"ERRO de conexão: {event.connection.remote_condition}")

    def on_link_error(self, event):
        log("PUB", f"ERRO no link: {event.link.remote_condition}")

    def on_rejected(self, event):
        log("PUB", "mensagem REJEITADA pelo broker")

    def on_transport_error(self, event):
        log("PUB", f"ERRO de transporte: {event.transport.condition}")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--address", default="queue://demo")
    p.add_argument("--count", type=int, default=5)
    p.add_argument("--interval", type=float, default=60)
    p.add_argument("--persistent", action="store_true")
    a = p.parse_args()
    Container(Publisher(a.address, a.count, a.interval, a.persistent)).run()
    log("PUB", "finalizado")
