public class Response1 {

    /// Handler
    ///
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

            // Validate 'name' is not blank if provided
            if (name != null && name.isBlank()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Query parameter 'name' must not be blank if provided.");
            }

            // Validate 'companyId' belongs to an existing company if provided
            if (companyId != null && !userService.companyExists(companyId)) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(String.format("Company with ID '%s' was not found.", companyId));
            }

            try {
                List<User> users = userService.getUsers(name, companyId);
                return ResponseEntity.ok(users);
            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred while fetching users.");
            }
        }
    }

    /// Service
    ///
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
                String trimmedName = name.trim();
                spec = spec.and(hasNameMatch(trimmedName));
            }

            if (companyId != null) {
                spec = spec.and(belongsToCompany(companyId));
            }

            return userRepository.findAll(spec);
        }

        // Matches 'name' as a case-insensitive substring of firstName or lastName,
        // or as a case-insensitive exact match of either field.
        private Specification<User> hasNameMatch(String name) {
            return (root, query, criteriaBuilder) -> {
                String likePattern = "%" + name.toLowerCase() + "%";
                Predicate firstNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")), likePattern);
                Predicate lastNameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")), likePattern);
                return criteriaBuilder.or(firstNameMatch, lastNameMatch);
            };
        }

        private Specification<User> belongsToCompany(UUID companyId) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("company").get("id"), companyId);
        }
    }

    /// Repository
    ///
    @Repository
    public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    }

}
