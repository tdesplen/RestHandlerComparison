public class Response4 {

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
            try {
                // Validate 'name' parameter
                if (name != null && name.isBlank()) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "'name' parameter must not be blank if provided."));
                }

                // Validate 'companyId' parameter
                if (companyId != null && !userService.companyExists(companyId)) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Company with ID " + companyId + " not found."));
                }

                List<User> users = userService.getUsers(name, companyId);

                if (users.isEmpty()) {
                    return ResponseEntity.noContent().build(); // 204
                }

                return ResponseEntity.ok(users); // 200
            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "An unexpected error occurred."));
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

        public List<User> getUsers(String name, UUID companyId) {
            Specification<User> spec = Specification.where(null);

            if (name != null) {
                spec = spec.and(UserSpecifications.hasName(name));
            }
            if (companyId != null) {
                spec = spec.and(UserSpecifications.belongsToCompany(companyId));
            }

            return userRepository.findAll(spec);
        }
    }

    public class UserSpecifications {

        public static Specification<User> hasName(String name) {
            return (root, query, criteriaBuilder) -> {
                String pattern = "%" + name.toLowerCase() + "%";

                // Partial match on first/last name, or exact match on either
                Predicate partialFirst = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")), pattern);
                Predicate partialLast = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")), pattern);
                Predicate exactFirst = criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("firstName")), name.toLowerCase());
                Predicate exactLast = criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("lastName")), name.toLowerCase());

                return criteriaBuilder.or(partialFirst, partialLast, exactFirst, exactLast);
            };
        }

        public static Specification<User> belongsToCompany(UUID companyId) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("company").get("id"), companyId);
        }
    }

    /// Repository
    @Repository
    public interface UserRepository extends JpaRepository<User, UUID>,
            JpaSpecificationExecutor<User> {
    }
}
