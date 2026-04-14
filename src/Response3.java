public class Response3 {

    /// Handler
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

            // Validate 'name' length to avoid malformed or excessively large queries
            if (name != null && (name.isBlank() || name.length() > 100)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Invalid 'name' parameter: must be between 1 and 100 characters.");
            }

            try {
                List<UserDTO> users = userService.getUsers(name, companyId);

                if (users.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }

                return ResponseEntity.ok(users);

            } catch (CompanyNotFoundException e) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Company not found for ID: " + companyId);

            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred while fetching users.");
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

        public List<UserDTO> getUsers(String name, UUID companyId) {
            // Validate that the company exists before querying for its users
            if (companyId != null && !companyRepository.existsById(companyId)) {
                throw new CompanyNotFoundException("Company not found: " + companyId);
            }

            List<User> users = userRepository.findByFilters(name, companyId);

            return users.stream()
                    .map(UserDTO::fromEntity)
                    .collect(Collectors.toList());
        }
    }

    /// Repository
    public interface UserRepository extends JpaRepository<User, UUID> {

        @Query("""
        SELECT u FROM User u
        WHERE (:name IS NULL
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :name, '%')))
        AND   (:companyId IS NULL OR u.company.id = :companyId)
    """)
        List<User> findByFilters(
                @Param("name") String name,
                @Param("companyId") UUID companyId);
    }
}
