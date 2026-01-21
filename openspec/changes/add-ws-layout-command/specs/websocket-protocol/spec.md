## ADDED Requirements

### Requirement: Server-Initiated Layout Request
The Android WebSocket client SHALL respond to `:get-layout` commands from the server by capturing and returning the current UI hierarchy.

#### Scenario: Successful layout request
- **WHEN** server sends `{:type "get-layout"}`
- **AND** accessibility service is enabled
- **THEN** client SHALL capture current window using `LayoutInspector`
- **AND** client SHALL respond with `{:type "layout" :data {...}}` containing serialized NodeInfo tree

#### Scenario: Layout request without accessibility service
- **WHEN** server sends `:get-layout` command
- **AND** accessibility service is disabled or unavailable
- **THEN** client SHALL respond with error `{:type "error" :message "Accessibility service not enabled"}`

### Requirement: Layout Response Format
The layout response SHALL contain the complete UI hierarchy in a JSON-serializable format.

#### Scenario: Valid layout response structure
- **WHEN** layout is successfully captured
- **THEN** response SHALL include root node with `className`, `id`, `text`, `desc`, `bounds`, `clickable`, `depth`
- **AND** response SHALL include `children` array with nested node structures
- **AND** all node properties SHALL be JSON-serializable (strings, numbers, booleans, arrays, objects)

#### Scenario: Empty layout handling
- **WHEN** no active window is available
- **THEN** response SHALL include `{:type "layout" :data nil}` or error message

### Requirement: Server Layout Response Handler
The Clojure WebSocket server SHALL handle `:layout` responses sent by clients after receiving `:get-layout` command.

#### Scenario: Server receives layout response
- **WHEN** server receives `{:type "layout" :data layout}`
- **THEN** server SHALL parse and log the layout structure
- **AND** server MAY optionally save layout to `layouts/` directory with timestamp

### Requirement: Server Layout Command Sender
The Clojure WebSocket server SHALL provide a helper function to send `:get-layout` command to specific connected clients.

#### Scenario: Server sends get-layout command
- **WHEN** server calls `(send-get-layout client-id)`
- **THEN** server SHALL send `{:type "get-layout"}` to specified client
- **AND** server SHALL expect `:layout` response from that client

### Requirement: Bidirectional Communication Pattern
The WebSocket protocol SHALL support both client-initiated and server-initiated messages.

#### Scenario: Existing client messages continue working
- **WHEN** client sends `:test`, `:ping`, or other existing message types
- **THEN** server SHALL handle them as before (no breaking changes)

#### Scenario: Server can initiate new command types
- **WHEN** server sends any new command type to client
- **THEN** client SHALL process unknown commands with `reply! {:type "error" :message "Unknown command"}`
