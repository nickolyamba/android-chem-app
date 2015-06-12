import webapp2
from google.appengine.api import oauth

config = {'Root':'default'}

app = webapp2.WSGIApplication([
    ('/supplier', 'supplier.Supplier'),
    ('/chemical', 'chemical.Chemical'),
    ('/solution', 'solution.Solution'),
    ('/signin', 'signin.GetToken'),
    
], debug=True)

# Source: lecture + reading
# https://webapp-improved.appspot.com/guide/routing.html#guide-routing
# [0-9]+ matches one or more digits, /? matches 0 or 1 '/'
# : is given after name, <> place for regex
app.router.add(webapp2.Route(r'/supplier/<id:[0-9]+>', 'supplier.Supplier'))
app.router.add(webapp2.Route(r'/supplier/<id:[a-zA-Z-]*>', 'supplier.SupplierSearch'))
#app.router.add(webapp2.Route(r'/supplier/search', 'supplier.SupplierSearch'))
app.router.add(webapp2.Route(r'/chemical/<cid:[a-zA-Z0-9]*><:/+><:[a-zA-Z]*><:/*><sid:[a-zA-Z0-9]*>', 'chemical.ChemicalSupplier'))
app.router.add(webapp2.Route(r'/chemical/<cid:[a-zA-Z0-9]+>', 'chemical.EditDeleteChem'))
app.router.add(webapp2.Route(r'/solution/<id:[0-9]+>', 'solution.Solution'))

