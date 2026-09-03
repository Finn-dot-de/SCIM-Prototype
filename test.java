   @Override
    public ListResponse<UserResource> searchUsers(String filterString, int startIndex, int count) {
        
        List<UserEntity> allDbUsers = userRepository.findAll();
        List<UserResource> matchedUsers;

        // 1. Wenn kein Filter da ist, geben wir einfach alle zurück
        if (filterString == null || filterString.isBlank()) {
            matchedUsers = allDbUsers.stream()
                    .map(this::mapToUserResource)
                    .collect(Collectors.toList());
        } 
        // 2. Komplexe SCIM-Filterung anwenden
        else {
            try {
                // Den String in einen abstrakten Syntaxbaum parsen
                Filter scimFilter = Filter.fromString(filterString);
                FilterEvaluator evaluator = new FilterEvaluator();

                matchedUsers = allDbUsers.stream()
                        .filter(dbUser -> {
                            try {
                                // JSON-String aus DB direkt in Jackson JsonNode umwandeln
                                JsonNode userNode = JsonUtils.getObjectReader().readTree(dbUser.getScimData());
                                
                                // Das SDK prüft vollautomatisch, ob der User zum Filter passt!
                                return scimFilter.visit(evaluator, userNode);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .map(this::mapToUserResource)
                        .collect(Collectors.toList());

            } catch (Exception e) {
                // Wenn das UEM einen syntaktisch falschen Filter schickt, werfen wir 400 Bad Request
                throw new IllegalArgumentException("Ungültiger SCIM-Filter: " + e.getMessage());
            }
        }

        // 3. Paginierung anwenden (SCIM startIndex ist 1-basiert, nicht 0-basiert!)
        int fromIndex = Math.max(0, startIndex - 1);
        int toIndex = Math.min(matchedUsers.size(), fromIndex + count);
        
        List<UserResource> pagedResults = (fromIndex <= matchedUsers.size()) 
                ? matchedUsers.subList(fromIndex, toIndex) 
                : List.of();

        // 4. In die SCIM-konforme ListResponse verpacken
        return new ListResponse<>(
                matchedUsers.size(), // Total Results (für das UEM wichtig)
                pagedResults,
                startIndex,
                count
        );
    }



import com.unboundid.scim2.common.messages.ListResponse;
// ... (andere Imports)

    @GetMapping
    @Operation(summary = "Benutzer suchen & filtern", description = "Unterstützt komplexe SCIM-Filter und Paginierung.")
    public ResponseEntity<ListResponse<UserResource>> searchUsers(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false, defaultValue = "1") int startIndex,
            @RequestParam(required = false, defaultValue = "100") int count) {

        log.info("Suche Users. Filter: '{}', Start: {}, Count: {}", filter, startIndex, count);

        ListResponse<UserResource> response = scimUserService.searchUsers(filter, startIndex, count);
        return ResponseEntity.ok(response);
    }
