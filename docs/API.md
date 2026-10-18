# Developer guide

Follow these pages in order for your first integration:

1. [Setup](getting-started.md): install the dependency, find TNE, and read a balance.
2. [Payments](payments.md): create an account, move money, and recover a lost response.
3. [Example](../examples/README.md): build and run a complete plugin.

Keep the [reference](reference.md) open while coding. It lists common methods,
failure codes, threading rules, and installation alternatives.

## Add features as you need them

| Your feature | Guide |
| --- | --- |
| Funds held until an action finishes | [Reservations](API_WORKFLOWS.md#reserve-money-before-committing-a-payment) |
| Delivery that may be interrupted | [Workflows](API_WORKFLOWS.md) |
| Recurring payments | [Schedules](API_WORKFLOWS.md) |
| Account restrictions, metadata, or spending allowances | [Account controls](API_ACCESS.md) |
| Partial refunds and disputes | [Refunds](API_ACCESS.md) |
| Player-approved invoices, conversion, notes, or GUI menus | [Commerce](API_COMMERCE.md) |
| Transaction notifications and custom value stores | [Extensions](API_EXTENSIONS.md) |
| Tests without a running Minecraft server | [Testing](API_EXTENSIONS.md#test-without-a-minecraft-server) |

These guides target API 1.0.64, Java 25, and a supported Paper/Folia server with
TNE installed. Check `api.capabilities()` before using optional services.
Your plugin owns player authorization and gameplay delivery; TNE owns the ledger
commit. Each guide explains the boundary for its operations.
