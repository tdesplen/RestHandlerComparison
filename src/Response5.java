public class Response5 {

    ///  Handler
    @RestController
    @RequestMapping("/api/users")
    public class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
            this.userService = userService;
        }

        @GetMapping
        public ResponseEntity<?> getUsers(
                @RequestParam(required = false) String name,
                @RequestParam(required = false) UUID companyId) {
            try {
                // Validate companyId references a real company (if provided)
                if (companyId != null && !userService.companyExists(companyId)) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Company not found with ID: " + companyId));
                }

                List<UserDTO> users = userService.getUsers(name, companyId);

                if (users.isEmpty()) {
                    return ResponseEntity.noContent().build(); // 204
                }

                return ResponseEntity.ok(users); // 200

            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", e.getMessage())); // 400

            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "An unexpected error occurred")); // 500
            }
        }
    }

    /// Service
    @Service
    public class UserService {

        private final UserRepository userRepository;
        private final CompanyRepository companyRepository;

        public UserService(UserRepository userRepository, CompanyRepository companyRepository) {
            this.userRepository = userRepository;
            this.companyRepository = companyRepository;
        }

        public boolean companyExists(UUID companyId) {
            return companyRepository.existsById(companyId);
        }

        public List<UserDTO> getUsers(String name, UUID companyId) {
            List<User> users;

            boolean hasName = name != null && !name.isBlank();
            boolean hasCompany = companyId != null;

            if (hasName && hasCompany) {
                users = userRepository.findByNameContainingAndCompanyId(name.trim(), companyId);
            } else if (hasName) {
                users = userRepository.findByNameContaining(name.trim());
            } else if (hasCompany) {
                users = userRepository.findByCompanyId(companyId);
            } else {
                users = userRepository.findAll();
            }

            return users.stream()
                    .map(UserDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /// Repository
    @Repository
    public interface UserRepository extends JpaRepository<User, UUID> {

        // Both filters: name match + company
        @Query("""
            SELECT u FROM User u
            WHERE u.company.id = :companyId
            AND (
                LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) = LOWER(:name)
            )
            """)
        List<User> findByNameContainingAndCompanyId(
                @Param("name") String name,
                @Param("companyId") UUID companyId);

        // Name filter only
        @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) = LOWER(:name)
            """)
        List<User> findByNameContaining(@Param("name") String name);

        // Company filter only
        List<User> findByCompanyId(UUID companyId);
    }
}
