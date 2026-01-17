Scheduler / REST 
↓
JobLauncher
↓
Job: documentAccessJob
↓
Step: documentAccessStep
↓
CHUNK LOOP
┌──────────────────────────────────────────┐
│ 1. reader.read()                          │
│ 2. processor.process(item)               │
│ 3. writer.write(chunk)                   │
│    → producer.send(dto)                  │
└──────────────────────────────────────────┘
↓
Repeat until reader returns null
↓
Step completes
↓
Job completes
