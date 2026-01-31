length = 20
rule "charset" {
  charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
}
rule "charset" {
  charset = "!@#$%^&*"
  min-chars = 1
}
