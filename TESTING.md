# Stratégie de test

## 1. Tests unitaires
- Utilisation de JUnit 5 et Mockito pour mocker les dépendances.
- Exemple : `KycServiceImplTest` dans `prs-service-impl`.

## 2. Tests d'intégration
- Utilisation de JUnit 5 et TestContainers pour tester les repositories avec une base PostgreSQL éphémère.
- Exemple : `PersonalInfoRepositoryTest` dans `prs-db-repository`.

## 3. Tests API
- Utilisation de JUnit 5 et MockMvc pour tester les contrôleurs REST.
- Exemple : `KycRestServerTest` dans `prs-rest-server`.

## 4. Couverture de code
- Utilisation du plugin Maven JaCoCo (défini dans le pom parent) pour générer les rapports de couverture.
- Commande : `mvn verify` pour générer le rapport dans `target/site/jacoco`.

## 5. Pipeline CI
- Le pipeline GitHub Actions (`.github/workflows/develop.yaml`) exécute tous les tests (unitaires, intégration, API) via `mvn verify`.
- La couverture de code est collectée à chaque exécution du pipeline.

## 6. Bonnes pratiques
- Les tests unitaires doivent mocker toutes les dépendances externes.
- Les tests d'intégration doivent utiliser TestContainers pour garantir l'isolation.
- Les tests API doivent vérifier les statuts HTTP et le contenu JSON.
- Viser une couverture de code élevée (>80%). 